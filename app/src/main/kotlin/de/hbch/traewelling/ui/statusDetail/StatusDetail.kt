package de.hbch.traewelling.ui.statusDetail

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.hbch.traewelling.R
import de.hbch.traewelling.api.models.status.Status
import de.hbch.traewelling.api.models.status.StatusVisibility
import de.hbch.traewelling.api.models.user.User
import de.hbch.traewelling.shared.LoggedInUserViewModel
import de.hbch.traewelling.shared.SettingsViewModel
import de.hbch.traewelling.theme.LocalFont
import de.hbch.traewelling.theme.MainTheme
import de.hbch.traewelling.theme.PolylineColor
import de.hbch.traewelling.ui.composables.ButtonWithIconAndText
import de.hbch.traewelling.ui.composables.DataLoading
import de.hbch.traewelling.ui.composables.OpenRailwayMapView
import de.hbch.traewelling.ui.composables.ProfilePicture
import de.hbch.traewelling.ui.tag.StatusTags
import de.hbch.traewelling.ui.composables.getBoundingBoxFromPolyLines
import de.hbch.traewelling.ui.composables.getPolyLinesFromFeatureCollection
import de.hbch.traewelling.ui.include.status.CheckInCard
import de.hbch.traewelling.ui.include.status.CheckInCardViewModel
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.time.format.DateTimeFormatter

@Composable
fun StatusDetail(
    statusId: Int,
    joinConnection: (Status) -> Unit,
    modifier: Modifier = Modifier,
    statusLoaded: (Status) -> Unit = { },
    statusDeleted: (Status) -> Unit = { },
    statusEdit: (Status) -> Unit = { },
    loggedInUserViewModel: LoggedInUserViewModel? = null,
    userSelected: (String, Boolean, Boolean) -> Unit = { _, _, _ -> }
) {
    val statusDetailViewModel: StatusDetailViewModel = viewModel()
    val checkInCardViewModel: CheckInCardViewModel = viewModel()
    var mapExpanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Status?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
    val displayTagsInCard by settingsViewModel.displayTagsInCard.observeAsState(true)

    LaunchedEffect(status) {
        if (status == null) {
            statusDetailViewModel.getStatusById(statusId, {
                operator = it.journey.operator?.name
                status = it
                statusLoaded(it)
            }, { })
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val mapModifier = modifier
            .fillMaxHeight(if (mapExpanded) 1.0f else 0.5f)
            .animateContentSize()
        Box(
            modifier = modifier
        ) {
            StatusDetailMap(
                modifier = mapModifier.align(Alignment.TopCenter),
                statusId = statusId,
                statusDetailViewModel = statusDetailViewModel
            )
            IconToggleButton(
                modifier = Modifier.align(Alignment.TopEnd),
                checked = mapExpanded,
                onCheckedChange = {
                    mapExpanded = it
                },
                colors = IconButtonDefaults.filledIconToggleButtonColors()
            ) {
                AnimatedContent(mapExpanded, label = "MapExpansionIcon") {
                    val iconSource =
                        if (it) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
                    Icon(
                        painter = painterResource(id = iconSource),
                        contentDescription = null
                    )
                }
            }
        }
        AnimatedVisibility (!mapExpanded) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CheckInCard(
                    checkInCardViewModel = checkInCardViewModel,
                    status = status,
                    joinConnection = joinConnection,
                    loggedInUserViewModel = loggedInUserViewModel,
                    onDeleted = statusDeleted,
                    handleEditClicked = statusEdit,
                    displayLongDate = true,
                    userSelected = userSelected
                )
                if (!displayTagsInCard && status != null) {
                    StatusTags(
                        statusId = statusId,
                        modifier = Modifier.fillMaxWidth(),
                        isOwnStatus = (loggedInUserViewModel?.loggedInUser?.value?.id
                            ?: -1) == status?.user?.id,
                        defaultVisibility = loggedInUserViewModel?.defaultStatusVisibility
                            ?: StatusVisibility.PUBLIC,
                        tags = status?.tags ?: listOf()
                    )
                }
                status?.likes?.let {
                    if (it > 0) {
                        StatusLikes(
                            statusId = statusId,
                            likes = it,
                            statusDetailViewModel = statusDetailViewModel,
                            modifier = Modifier.fillMaxWidth(),
                            userSelected = userSelected
                        )
                    }
                }
                ButtonWithIconAndText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.open_with_bahnexpert),
                    drawableId = R.drawable.ic_train,
                    onClick = {
                        val dStatus = status
                        if (dStatus != null) {
                            val intent = CustomTabsIntent.Builder()
                                .setShowTitle(false)
                                .build()

                            val isoDate = DateTimeFormatter.ISO_INSTANT.format(dStatus.journey.origin.departurePlanned)

                            val uri = Uri.Builder()
                                .scheme("https")
                                .authority("bahn.expert")
                                .appendPath("details")
                                .appendPath(dStatus.journey.journeyNumber.toString())
                                .appendPath(isoDate)
                                .appendQueryParameter("station", dStatus.journey.origin.evaIdentifier.toString())
                                .build()

                            intent.launchUrl(
                                context,
                                uri
                            )
                        }
                    }
                )
                if (operator != null) {
                    Text(
                        text = operator ?: "",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = LocalFont.current.labelMedium
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (status?.isTraewelldroidCheckIn == true) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = stringResource(
                            id = R.string.checked_in_with,
                            status?.client?.name ?: "Träwelling"
                        ),
                        style = LocalFont.current.labelMedium,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDetailMap(
    modifier: Modifier = Modifier,
    statusId: Int,
    statusDetailViewModel: StatusDetailViewModel
) {
    val color = PolylineColor.toArgb()
    val polyLines = remember { mutableStateListOf<Polyline>() }
    var requested by remember { mutableStateOf(false) }
    var mapView: MapView? = remember { null }

    LaunchedEffect(requested) {
        if (!requested && polyLines.isEmpty()) {
            requested = true
            statusDetailViewModel.getPolylineForStatus(
                statusId = statusId,
                successfulCallback = {
                    polyLines.addAll(getPolyLinesFromFeatureCollection(it, color))
                },
                failureCallback = {}
            )
        }
    }

    LaunchedEffect(polyLines.size) {
        val map = mapView
        if (polyLines.isNotEmpty() && map != null) {
            map.overlayManager.overlays().removeIf {
                it is Polyline
            }
            map.overlayManager.overlays().addAll(polyLines)

            val bounds = getBoundingBoxFromPolyLines(polyLines)
            map.zoomToBoundingBox(bounds.increaseByScale(1.1f), false)
        }
    }

    if (polyLines.isNotEmpty()) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth(),
        ) {
            OpenRailwayMapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                onLoad = {
                    mapView = it
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusLikes(
    statusId: Int,
    likes: Int,
    statusDetailViewModel: StatusDetailViewModel,
    modifier: Modifier = Modifier,
    userSelected: (String, Boolean, Boolean) -> Unit = { _, _, _ -> }
) {
    var cardExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val expandAction: () -> Unit = { cardExpanded = !cardExpanded }
    val likeUsers = remember { mutableStateListOf<User>() }

    LaunchedEffect(cardExpanded) {
        if (cardExpanded && likeUsers.size == 0) {
            statusDetailViewModel.getLikesForStatus(
                statusId,
                {
                    likeUsers.clear()
                    likeUsers.addAll(it)
                    isLoading = false
                },
                {
                    isLoading = false
                }
            )
        }
    }

    ElevatedCard(
        onClick = expandAction,
        modifier = modifier
    ) {
        var contentModifier = Modifier.padding(horizontal = 16.dp)
        if (cardExpanded) {
            contentModifier = contentModifier.padding(bottom = 8.dp)
        }
        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.likes, likes),
                    style = LocalFont.current.bodyLarge
                )
                IconButton(onClick = expandAction) {
                    AnimatedContent(cardExpanded, label = "CardExpansionIcon") {
                        val icon =
                            if (it)
                                R.drawable.ic_expand_less
                            else
                                R.drawable.ic_expand_more
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null
                        )
                    }
                }
            }
            AnimatedVisibility (cardExpanded) {
                if (isLoading) {
                    DataLoading()
                } else {
                    if (likeUsers.size > 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            likeUsers.forEach {
                                Liker(
                                    user = it,
                                    modifier = Modifier.fillMaxWidth(),
                                    userSelected = userSelected
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Liker(
    user: User,
    modifier: Modifier = Modifier,
    userSelected: (String, Boolean, Boolean) -> Unit = { _, _, _ -> }
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { userSelected(user.username, user.privateProfile, user.following) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ProfilePicture(
                user = user,
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp)
            )
            Text(
                text = "@${user.username}",
                fontWeight = FontWeight.ExtraBold
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_select),
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun StatusDetailPreview() {
    MainTheme {
        StatusDetail(
            modifier = Modifier.fillMaxWidth(),
            statusId = 1117900,
            joinConnection = { }
        )
    }
}
