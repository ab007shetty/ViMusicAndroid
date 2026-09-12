package com.abshetty.vimusic.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abshetty.vimusic.core.designsystem.vimusic.ConfirmDialog
import com.abshetty.vimusic.core.designsystem.vimusic.HapticSlider
import com.abshetty.vimusic.core.data.sync.SyncPhase
import com.abshetty.vimusic.core.data.sync.SyncState
import com.abshetty.vimusic.core.designsystem.vimusic.Header
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.NavigationRail
import com.abshetty.vimusic.core.designsystem.vimusic.RailIconButton
import com.abshetty.vimusic.core.designsystem.vimusic.SettingsDescription
import com.abshetty.vimusic.core.designsystem.vimusic.SettingsEntry
import com.abshetty.vimusic.core.designsystem.vimusic.SettingsGroup
import com.abshetty.vimusic.core.designsystem.vimusic.SettingsSwitch
import com.abshetty.vimusic.core.designsystem.vimusic.SettingsWarning
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold
import androidx.compose.material.icons.rounded.Check
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable

private const val WEB_APP_URL = "https://vimusic.vercel.app"

private const val SOURCE_URL = "https://github.com/ab007shetty/ViMusicAndroid"

private enum class Confirmation(
    val title: String,
    val consequence: String,
    val confirmLabel: String,
    val destructive: Boolean = true,
) {
    CLEAR_IMAGES(
        title = "Clear cached artwork?",
        consequence = "Every thumbnail is deleted from this device. Nothing in your library " +
            "changes; images re-download as you scroll, which costs a little data.",
        confirmLabel = "Clear",
    ),
    IMPORT(
        title = "Import a database?",
        consequence = "Songs and playlists from the file are merged into this library. " +
            "Nothing already here is deleted, but anything the file also contains will be " +
            "overwritten with the file's version, and the merge cannot be undone. " +
            "Export a backup first if you are unsure.",
        confirmLabel = "Choose file",
        destructive = false,
    ),
    SIGN_OUT(
        title = "Sign out?",
        consequence = "The shared library disappears from this device and the app returns to " +
            "the guest view. Nothing is deleted on the server, and signing back in restores " +
            "it. Cached audio stays.",
        confirmLabel = "Sign out",
    ),
}

private enum class SettingsSection(val label: String) {
    ACCOUNT("Account"),
    APPEARANCE("Appearance"),
    PLAYER("Player"),
    CACHE("Cache"),
    DATABASE("Database"),
    OTHER("Other"),
    ABOUT("About"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val amoled by viewModel.amoled.collectAsStateWithLifecycle()
    val pinColours by viewModel.pinColours.collectAsStateWithLifecycle()
    val holdMs by viewModel.holdThresholdMs.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()
    val androidAuto by viewModel.androidAuto.collectAsStateWithLifecycle()
    val pauseHistory by viewModel.pauseSearchHistory.collectAsStateWithLifecycle()
    val historyCount by viewModel.searchHistoryCount.collectAsStateWithLifecycle()
    val spatial by viewModel.spatialAudio.collectAsStateWithLifecycle()
    val volumeSkipReady by viewModel.volumeSkipReady.collectAsStateWithLifecycle()
    val imageCacheEnabled by viewModel.imageCacheEnabled.collectAsStateWithLifecycle()
    val imageCacheSummary by viewModel.imageCacheSummary.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val (colorPalette, typography) = LocalAppearance.current

    var section by remember { mutableStateOf(SettingsSection.ACCOUNT) }
    var confirm by remember { mutableStateOf<Confirmation?>(null) }

    LaunchedEffect(section) {
        viewModel.refreshVolumeSkip()
        viewModel.refreshSpatial()
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let(viewModel::exportTo) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importFrom) }

    confirm?.let { pending ->
        ConfirmDialog(
            title = pending.title,
            consequence = pending.consequence,
            confirmLabel = pending.confirmLabel,
            destructive = pending.destructive,
            onDismiss = { confirm = null },
            onConfirm = {
                when (pending) {
                    Confirmation.CLEAR_IMAGES -> viewModel.clearImageCache()
                    Confirmation.IMPORT -> importLauncher.launch(arrayOf("*/*"))
                    Confirmation.SIGN_OUT -> scope.launch { viewModel.signOut() }
                }
            },
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val sections = SettingsSection.entries

    Row(modifier.fillMaxSize().background(colorPalette.background0)) {
        NavigationRail(
            topIcon = Icons.Rounded.ArrowBack,
            onTopIconClick = onBack,
            tabIndex = sections.indexOf(section),
            onTabIndexChange = { section = sections[it] },
            isLandscape = isLandscape,
            showIcons = false,

            topSpace = 56.dp,
        ) { Item ->
            sections.forEachIndexed { index, s ->
                Item(index, s.label, s.icon())
            }
        }

        Column(Modifier.fillMaxSize()) {
            Header(title = section.label)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                when (section) {
                    SettingsSection.ACCOUNT -> {
                        val current = account
                        SettingsGroup("Google account")
                        if (current == null) {
                            SettingsDescription(
                                "Signed out. Your library is on this device only. Sign in to " +
                                    "share it with the web app."
                            )
                            SettingsEntry(
                                title = "Sign in with Google",
                                text = "Uses the same account as the website",
                                onClick = { scope.launch { viewModel.signIn(context) } },
                            )
                        } else {
                            SettingsEntry(title = current.email, text = "Signed in")
                            SettingsDescription(
                                "Favourites, playlists and listening time sync with the web app."
                            )

                            SettingsEntry(
                                title = "Sync",
                                text = syncSummary(sync),
                                enabled = false,
                                trailing = if (sync.isSyncing) {
                                    {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = colorPalette.textDisabled,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                            )
                            SettingsEntry(
                                title = "Sign out",
                                text = "Keeps this device's cache, hides the shared library",
                                onClick = { confirm = Confirmation.SIGN_OUT },
                            )
                        }
                    }

                    SettingsSection.APPEARANCE -> {
                        SettingsGroup("Colors")
                        SettingsSwitch(
                            "Pure black background",
                            "Saves power on OLED screens",
                            amoled,
                        ) { viewModel.setAmoled(it) }
                        SettingsSwitch(
                            "Keep colours fixed",
                            "Stops the interface recolouring itself from the album art",
                            pinColours,
                        ) { viewModel.setPinColours(it) }
                    }

                    SettingsSection.PLAYER -> {
                        SettingsGroup("Equalizer")
                        SettingsDescription(
                            "Bands, presets, bass, stereo and mono apply to songs stored " +
                                "on this device. Streamed songs play through YouTube's own " +
                                "player, which hands the audio straight to the system -- " +
                                "nothing here can reach it, so the equalizer has no effect " +
                                "on them."
                        )
                        SettingsEntry(
                            title = "Open equalizer",
                            text = "Ten bands, presets, tone and stereo controls",
                            onClick = onOpenEqualizer,
                        )

                        SettingsGroup("Volume key skip")
                        SettingsDescription(
                            "On the lock screen, hold volume up for the next track and " +
                                "volume down for the previous one. The volume itself does " +
                                "not move; a short press still changes it as usual. On an " +
                                "unlocked screen the keys are left alone -- there is a skip " +
                                "button right there. It cannot work with the display fully " +
                                "off: Android does not deliver volume keys to any app while " +
                                "the screen is asleep."
                        )
                        SettingsEntry(title = "Hold time", text = holdMs.toString() + " ms")
                        HapticSlider(
                            value = holdMs.toFloat(),
                            onValueChange = { viewModel.setHoldThreshold(it.toLong()) },
                            valueRange = 250f..1200f,
                            steps = 18,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        SettingsDescription(
                            "Shorter is quicker to trigger; longer is harder to trigger by " +
                                "accident."
                        )
                        SettingsEntry(
                            title = if (volumeSkipReady) "Volume key skip is on"
                            else "Turn on volume key skip",
                            text = if (volumeSkipReady) {
                                "Working. Android switches this off again whenever the " +
                                    "app is updated, so check here if skipping stops."
                            } else {
                                "Accessibility -> Installed apps -> ViMusic. If Android " +
                                    "calls it a restricted setting, open app info, tap the " +
                                    "three dots, and allow restricted settings first."
                            },
                            onClick = { viewModel.openAccessibilitySettings(context) },
                        )
                    }

                    SettingsSection.CACHE -> {
                        SettingsGroup("Thumbnails")
                        SettingsDescription(
                            "Cover art is kept on this device so a list you have already " +
                                "scrolled does not fetch every picture again. Audio is not " +
                                "cached at all: it plays inside YouTube's own player, and " +
                                "nothing passes through this app to keep."
                        )
                        SettingsSwitch(
                            "Keep thumbnails on disk",
                            "Off means every cover is fetched again each launch. Takes " +
                                "effect next time the app starts.",
                            imageCacheEnabled,
                        ) { viewModel.setImageCacheEnabled(it) }
                        SettingsEntry(title = "Used", text = imageCacheSummary)
                        SettingsEntry(
                            title = "Clear thumbnails",
                            text = "Frees the space now; covers reload as you scroll",
                            onClick = { confirm = Confirmation.CLEAR_IMAGES },
                        )
                    }

                    SettingsSection.DATABASE -> {
                        SettingsGroup("Backup")
                        SettingsDescription(
                            "Exports a .db file in the same format as the web app and the " +
                                "original ViMusic, so backups move freely between them."
                        )
                        SettingsEntry(
                            title = "Export",
                            text = "Save a copy of the library",
                            onClick = { exportLauncher.launch(viewModel.exportFileName()) },
                        )
                        SettingsGroup("Restore")
                        SettingsDescription("Importing merges rather than replaces.")
                        SettingsEntry(
                            title = "Import",
                            text = "Merge a .db file into this library",
                            onClick = { confirm = Confirmation.IMPORT },
                        )
                    }

                    SettingsSection.OTHER -> {
                        SettingsGroup("Android Auto")
                        SettingsDescription(
                            "Remember to enable \"Unknown sources\" in the Developer " +
                                "Settings of Android Auto."
                        )
                        SettingsSwitch(
                            "Android Auto",
                            "Enable Android Auto support",
                            androidAuto,
                        ) { viewModel.setAndroidAuto(it) }

                        SettingsGroup("Search history")
                        SettingsSwitch(
                            "Pause search history",
                            "Neither save new searched queries nor show history",
                            pauseHistory,
                        ) { viewModel.setPauseSearchHistory(it) }
                        SettingsEntry(
                            title = "Clear search history",
                            text = if (historyCount == 0) "History is empty"
                            else historyCount.toString() + " queries saved",
                            enabled = historyCount > 0,
                            onClick = viewModel::clearSearchHistory,
                        )

                        SettingsGroup("Spatial audio")
                        SettingsDescription(
                            "YouTube serves stereo, so there is no Atmos stream to decode. " +
                                "What this does is hand playback to the system spatializer " +
                                "-- the engine Samsung ships as Dolby Atmos -- which widens " +
                                "stereo on headphones that support it."
                        )
                        SettingsSwitch(
                            "Spatial audio",
                            spatial.reason,
                            spatial.enabled,
                            enabled = spatial.supported,
                        ) { viewModel.setSpatialAudio(it) }
                        if (spatial.supported && !spatial.available) {
                            SettingsEntry(
                                title = "System sound settings",
                                text = "Turn on spatial audio for this device",
                                onClick = { viewModel.openSoundSettings(context) },
                            )
                        }

                        SettingsGroup("Service lifetime")
                        SettingsWarning(
                            "If battery optimizations are applied, the playback notification " +
                                "can suddenly disappear when paused."
                        )
                        SettingsDescription(
                            "Some phones stop background audio to save power, even for a " +
                                "foreground media service. If playback cuts out when the " +
                                "screen is off, exempt ViMusic."
                        )
                        SettingsEntry(
                            title = "Ignore battery optimizations",
                            text = "Disable background restrictions",
                            onClick = { viewModel.openBatterySettings(context) },
                        )
                    }

                    SettingsSection.ABOUT -> {
                        SettingsGroup("ViMusic")
                        val update by viewModel.updateState.collectAsStateWithLifecycle()

                        SettingsEntry(
                            title = "Version",
                            text = when (val current = update) {
                                is UpdateState.Available ->
                                    viewModel.versionName + "  -  " +
                                        current.update.versionName + " is available"
                                UpdateState.Checking -> "Checking..."
                                UpdateState.Downloading -> "Downloading the update"
                                UpdateState.UpToDate -> viewModel.versionName + "  -  up to date"
                                is UpdateState.Failed -> current.message
                                UpdateState.Idle -> viewModel.versionName
                            },
                            trailing = {
                                val label = when (update) {
                                    is UpdateState.Available -> "Install"
                                    UpdateState.Checking, UpdateState.Downloading -> null
                                    else -> "Check now"
                                }

                                if (label == null) {
                                    CircularProgressIndicator(
                                        color = colorPalette.accent,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Text(
                                        label,
                                        style = typography.xxs.semiBold,
                                        color = colorPalette.text,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(colorPalette.background2)
                                            .clickable {
                                                if (update is UpdateState.Available) {
                                                    viewModel.installUpdate()
                                                } else {
                                                    viewModel.checkForUpdate()
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                    )
                                }
                            },
                        )

                        SettingsEntry(
                            title = "Source code",
                            text = "ab007shetty/ViMusicAndroid",
                            trailing = {
                                Icon(
                                    Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = colorPalette.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = { viewModel.openUrl(context, SOURCE_URL) },
                        )

                        SettingsGroup("Web app")
                        SettingsEntry(
                            title = "vimusic.vercel.app",
                            text = "The same library in a browser",
                            trailing = {
                                Icon(
                                    Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = colorPalette.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = { viewModel.openUrl(context, WEB_APP_URL) },
                        )

                        SettingsGroup("Author")
                        AUTHOR_LINKS.forEach { link ->
                            SettingsEntry(
                                title = link.label,
                                text = link.handle,
                                trailing = {
                                    Icon(
                                        link.icon,
                                        contentDescription = null,
                                        tint = colorPalette.accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = { viewModel.openUrl(context, link.url) },
                            )
                        }

                        SettingsGroup("Credits")
                        SettingsEntry(
                            title = "vfsfitvnm/ViMusic",
                            text = "The OG ViMusic",
                            onClick = {
                                viewModel.openUrl(context, "https://github.com/vfsfitvnm/ViMusic")
                            },
                        )
                        SettingsEntry(
                            title = "LRCLIB",
                            text = "Synced lyrics",
                            onClick = { viewModel.openUrl(context, "https://lrclib.net") },
                        )
                    }
                }
            }

            SnackbarHost(snackbar)
        }
    }
}

private data class AuthorLink(
    val label: String,
    val handle: String,
    val url: String,
    val icon: ImageVector,
)

private val AUTHOR_LINKS = listOf(
    AuthorLink(
        "Instagram", "@abshetr", "https://instagram.com/abshetr",
        BrandIcons.Instagram,
    ),
)

private fun SettingsSection.icon() = when (this) {
    SettingsSection.ACCOUNT -> Icons.Rounded.Person
    SettingsSection.APPEARANCE -> Icons.Rounded.Palette
    SettingsSection.PLAYER -> Icons.Rounded.PlayArrow
    SettingsSection.CACHE -> Icons.Rounded.SdStorage
    SettingsSection.DATABASE -> Icons.Rounded.Storage
    SettingsSection.OTHER -> Icons.Rounded.MoreHoriz
    SettingsSection.ABOUT -> Icons.Rounded.Info
}

private fun syncSummary(state: SyncState): String {
    val syncedAt = state.lastSyncedAtMs
    return when {
        state.isSyncing -> "Syncing..."
        state.phase == SyncPhase.FAILED ->
            "Last attempt failed: " + (state.message ?: "unknown error") + ". Retrying."
        syncedAt != null -> "Up to date - synced " + relativeTime(syncedAt)
        else -> "Waiting for a connection"
    }
}

private fun relativeTime(atMs: Long): String {
    val seconds = ((System.currentTimeMillis() - atMs) / 1000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> (seconds / 60).toString() + " min ago"
        seconds < 86_400 -> (seconds / 3600).toString() + " h ago"
        else -> (seconds / 86_400).toString() + " d ago"
    }
}
