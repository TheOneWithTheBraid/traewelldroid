package de.hbch.traewelling.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jcloquell.androidsecurestorage.SecureStorage
import de.c1710.filemojicompat_ui.views.picker.EmojiPackItemAdapter
import de.hbch.traewelling.R
import de.hbch.traewelling.shared.LineIcons
import de.hbch.traewelling.shared.LoggedInUserViewModel
import de.hbch.traewelling.shared.SettingsViewModel
import de.hbch.traewelling.shared.SharedValues
import de.hbch.traewelling.theme.LocalColorScheme
import de.hbch.traewelling.theme.LocalFont
import de.hbch.traewelling.theme.MainTheme
import de.hbch.traewelling.ui.composables.ButtonWithIconAndText
import de.hbch.traewelling.ui.composables.OpenRailwayMapLayer
import de.hbch.traewelling.ui.composables.SwitchWithIconAndText
import de.hbch.traewelling.util.getJwtExpiration
import de.hbch.traewelling.util.refreshJwt
import de.hbch.traewelling.util.getLocalDateTimeString
import de.hbch.traewelling.util.readOrDownloadLineIcons
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun Settings(
    snackbarHostState: SnackbarHostState,
    loggedInUserViewModel: LoggedInUserViewModel? = null,
    emojiPackItemAdapter: EmojiPackItemAdapter? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DisplayProviderSettings(
            snackbarHostState = snackbarHostState
        )
        CheckInProviderSettings(
            snackbarHostState = snackbarHostState,
            loggedInUserViewModel = loggedInUserViewModel
        )
        HashtagSettings(
            snackbarHostState = snackbarHostState
        )
        MapViewSettings()
        LineIconsSettings()
        EmojiSettings(
            emojiPackItemAdapter = emojiPackItemAdapter
        )
    }
}

@Composable
private fun DisplayProviderSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = viewModel(
        viewModelStoreOwner = context as ViewModelStoreOwner
    )
    val displayTagsInCheckInCard by settingsViewModel.displayTagsInCard.observeAsState(true)
    val displayJourneyNumber by settingsViewModel.displayJourneyNumber.observeAsState(true)
    val displayDivergentStop by settingsViewModel.displayDivergentStop.observeAsState(true)
    val useSystemFont by settingsViewModel.useSystemFont.observeAsState(false)

    SettingsCard(
        title = R.string.settings_display,
        description = R.string.settings_display_description,
        modifier = modifier,
        expandable = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SwitchWithIconAndText(
                checked = displayTagsInCheckInCard,
                onCheckedChange = {
                    settingsViewModel.updateDisplayTagsInCard(context, it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.changes_saved)
                        )
                    }
                },
                drawableId = R.drawable.ic_tag,
                stringId = R.string.settings_display_tags_in_card,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchWithIconAndText(
                checked = displayJourneyNumber,
                onCheckedChange = {
                    settingsViewModel.updateDisplayJourneyNumber(context, it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.changes_saved)
                        )
                    }
                },
                drawableId = R.drawable.ic_train,
                stringId = R.string.settings_display_journey_number,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchWithIconAndText(
                checked = displayDivergentStop,
                onCheckedChange = {
                    settingsViewModel.updateDisplayDivergentStop(context, it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.changes_saved)
                        )
                    }
                },
                drawableId = R.drawable.ic_navigation,
                stringId = R.string.settings_display_divergent_stop,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchWithIconAndText(
                checked = useSystemFont,
                onCheckedChange = {
                    settingsViewModel.updateUseSystemFont(context, it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.changes_saved)
                        )
                    }
                },
                drawableId = R.drawable.ic_font,
                stringId = R.string.use_system_font,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CheckInProviderSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    loggedInUserViewModel: LoggedInUserViewModel? = null
) {
    SettingsCard(
        modifier = modifier,
        title = R.string.settings_check_in_providers,
        description = R.string.settings_check_in_providers_description,
        expandable = true
    ) {
        TraewellingProviderSettings(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth(),
            loggedInUserViewModel = loggedInUserViewModel
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp)
        )
        TravelynxProviderSettings(
            snackbarHostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
    }
}

@Composable
private fun TraewellingProviderSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    loggedInUserViewModel: LoggedInUserViewModel? = null
) {
    if (loggedInUserViewModel != null) {
        val context = LocalContext.current
        val secureStorage = SecureStorage(context)
        var jwt by remember { mutableStateOf(secureStorage.getObject(SharedValues.SS_JWT, String::class.java) ?: "") }
        var defaultCheckIn by remember { mutableStateOf(secureStorage.getObject(SharedValues.SS_TRWL_AUTO_LOGIN, Boolean::class.java) ?: true) }
        val username by loggedInUserViewModel.username.observeAsState("")
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trwl),
                    contentDescription = null
                )
                Text(
                    text = "Träwelling",
                    style = LocalFont.current.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(id = R.string.signed_in_as, username)
            )
            Text(
                text = stringResource(id = R.string.jwt_expiration, getJwtExpiration(jwt = jwt))
            )
            SwitchWithIconAndText(
                checked = defaultCheckIn,
                onCheckedChange = {
                    defaultCheckIn = it
                    secureStorage.storeObject(SharedValues.SS_TRWL_AUTO_LOGIN, it)
                },
                drawableId = R.drawable.ic_check_in,
                stringId = R.string.auto_check_in
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ButtonWithIconAndText(
                    stringId = R.string.renew_login,
                    drawableId = R.drawable.ic_refresh,
                    onClick = {
                        context.refreshJwt {
                            jwt = it
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.renew_login_success))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                ButtonWithIconAndText(
                    modifier = Modifier.weight(1f),
                    stringId = R.string.logout,
                    drawableId = R.drawable.ic_logout,
                    onClick = {
                        loggedInUserViewModel.logoutWithRestart(context)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TravelynxProviderSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val secureStorage = SecureStorage(context)
    var token by remember { mutableStateOf(secureStorage.getObject(SharedValues.SS_TRAVELYNX_TOKEN, String::class.java) ?: "") }
    var defaultCheckIn by remember { mutableStateOf(secureStorage.getObject(SharedValues.SS_TRAVELYNX_AUTO_CHECKIN, Boolean::class.java) ?: false) }
    val saveTokenAction: () -> Unit = {
        keyboardController?.hide()
        secureStorage.storeObject(SharedValues.SS_TRAVELYNX_TOKEN, token)
        SharedValues.TRAVELYNX_TOKEN = token
        coroutineScope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.changes_saved))
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_travelynx),
                contentDescription = null
            )
            Text(
                text = "travelynx",
                style = LocalFont.current.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f),
                value = token,
                singleLine = true,
                onValueChange = {
                    token = it
                },
                label = {
                    Text(
                        text = stringResource(id = R.string.travelynx_token)
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.travelynx_token)
                    )
                }
            )
            FilledIconButton(
                onClick = saveTokenAction
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_in),
                    contentDescription = null
                )
            }
        }
        SwitchWithIconAndText(
            checked = defaultCheckIn,
            onCheckedChange = {
                defaultCheckIn = it
                secureStorage.storeObject(SharedValues.SS_TRAVELYNX_AUTO_CHECKIN, it)
            },
            drawableId = R.drawable.ic_check_in,
            stringId = R.string.auto_check_in
        )
        Text(
            text = stringResource(id = R.string.travelynx_limited_functionality),
            style = LocalFont.current.labelSmall,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HashtagSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hashtagText by remember { mutableStateOf("") }
    @Suppress("CanBeVal") var secureStorage: SecureStorage?
    var saveHashtagAction: () -> Unit = { }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    if (!LocalView.current.isInEditMode) {
        secureStorage = SecureStorage(context)
        hashtagText = secureStorage.getObject(SharedValues.SS_HASHTAG, String::class.java) ?: ""
        saveHashtagAction = {
            keyboardController?.hide()
            secureStorage.storeObject(SharedValues.SS_HASHTAG, hashtagText)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.changes_saved))
            }
        }
    }

    SettingsCard(
        modifier = modifier,
        title = R.string.hashtag,
        description = R.string.default_hashtag_text,
        expandable = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f),
                value = hashtagText,
                singleLine = true,
                onValueChange = {
                    hashtagText = it
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_hashtag),
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = R.string.hashtag)
                    )
                }
            )
            FilledIconButton(
                onClick = saveHashtagAction
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_in),
                    contentDescription = stringResource(id = R.string.store_hashtag)
                )
            }
        }
    }
}

@Composable
private fun MapViewSettings(
    modifier: Modifier = Modifier
) {
    var secureStorage: SecureStorage? = null
    var selectedOrmLayer by remember { mutableStateOf(OpenRailwayMapLayer.STANDARD) }

    if (!LocalView.current.isInEditMode) {
        secureStorage = SecureStorage(LocalContext.current)
        val storedOrmLayer = secureStorage.getObject(SharedValues.SS_ORM_LAYER, OpenRailwayMapLayer::class.java)
        storedOrmLayer?.let {
            selectedOrmLayer = it
        }
    }

    SettingsCard(
        modifier = modifier,
        title = R.string.map_view,
        description = R.string.configure_map,
        expandable = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(id = R.string.openrailwaymap)
                )
                OpenRailwayMapLayer.entries.forEach { layer ->
                    val layerSelected : () -> Unit = {
                        selectedOrmLayer = layer
                        secureStorage?.storeObject(SharedValues.SS_ORM_LAYER, selectedOrmLayer)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = layerSelected),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOrmLayer == layer,
                            onClick = layerSelected
                        )
                        Column {
                            Text(
                                text = stringResource(id = layer.title),
                                style = LocalFont.current.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = stringResource(id = layer.description),
                                style = LocalFont.current.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiSettings(
    modifier: Modifier = Modifier,
    emojiPackItemAdapter: EmojiPackItemAdapter? = null
) {
    SettingsCard(
        modifier = modifier,
        title = R.string.emoji,
        description = R.string.emoji_text,
        expandable = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (emojiPackItemAdapter != null) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val recyclerView = RecyclerView(context, null)

                        recyclerView.layoutManager = LinearLayoutManager(context)
                        recyclerView.adapter = emojiPackItemAdapter
                        recyclerView.isNestedScrollingEnabled = false

                        recyclerView
                    }
                )
            }
        }
    }
}

@Composable
private fun LineIconsSettings(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val file by remember { mutableStateOf(File(context.filesDir, "line-colors.csv")) }
    var lastChanged by remember {
        mutableStateOf(
            Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault())
        )
    }
    var isLoading by remember { mutableStateOf(false) }

    SettingsCard(
        title = R.string.line_icons,
        description = R.string.update_line_icons,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    id = R.string.last_updated,
                    getLocalDateTimeString(lastChanged)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            ButtonWithIconAndText(
                stringId = R.string.refresh,
                drawableId = R.drawable.ic_download,
                isLoading = isLoading,
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        val icons = async {
                            context.readOrDownloadLineIcons(true)
                        }
                        LineIcons.getInstance().icons.clear()
                        LineIcons.getInstance().icons.addAll(icons.await())
                        isLoading = false
                        lastChanged = ZonedDateTime.now(ZoneId.systemDefault())
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    expandable: Boolean = false,
    @StringRes title: Int,
    @StringRes description: Int,
    content: @Composable (ColumnScope.() -> Unit)
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier,
        onClick = {
            if (expandable)
                expanded = !expanded
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = title),
                        style = LocalFont.current.headlineSmall
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(id = description),
                        style = LocalFont.current.labelSmall
                    )
                }
                if (expandable) {
                    IconToggleButton(
                        checked = expanded,
                        onCheckedChange = { expanded = !expanded }
                    ) {
                        val icon = if (expanded)
                            R.drawable.ic_expand_less
                        else
                            R.drawable.ic_expand_more

                        Icon(
                            painterResource(id = icon),
                            contentDescription = null
                        )
                    }
                }
            }
            AnimatedVisibility((expandable && expanded) || !expandable) {
                Card(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = LocalColorScheme.current.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsCardPreview() {
    MainTheme {
        val title = R.string.hashtag
        val description = R.string.default_hashtag_text
        val content: @Composable ColumnScope.() -> Unit = {
            OutlinedTextField(
                value = "Test",
                onValueChange = { },
                label = {
                    Text(
                        text = stringResource(id = title)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                expandable = true,
                title = title,
                description = description,
                content = content
            )

            SettingsCard(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                expandable = false,
                title = title,
                description = description,
                content = content
            )
        }
    }
}
