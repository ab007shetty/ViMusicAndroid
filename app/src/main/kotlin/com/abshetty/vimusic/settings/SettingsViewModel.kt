package com.abshetty.vimusic.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abshetty.vimusic.core.data.auth.Account
import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.data.backup.DatabaseBackup
import com.abshetty.vimusic.core.data.sync.LibrarySyncCoordinator
import com.abshetty.vimusic.core.data.sync.SyncState
import com.abshetty.vimusic.core.data.sync.SyncStatus
import com.abshetty.vimusic.core.datastore.SettingsStore
import com.abshetty.vimusic.core.data.repository.SearchHistoryRepository
import com.abshetty.vimusic.core.media.audio.SpatialAudio
import com.abshetty.vimusic.core.media.audio.SpatialState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: AuthRepository,
    private val settings: SettingsStore,
    private val backup: DatabaseBackup,
    private val librarySync: LibrarySyncCoordinator,
    syncStatus: SyncStatus,
    private val searchHistory: SearchHistoryRepository,
    private val updates: com.abshetty.vimusic.core.data.update.UpdateRepository,
    private val spatial: SpatialAudio,
) : ViewModel() {
    val account: StateFlow<Account?> = auth.account

    val amoled: StateFlow<Boolean> = settings.amoled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pinColours: StateFlow<Boolean> = settings.pinColours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val holdThresholdMs: StateFlow<Long> = settings.holdThresholdMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 450L)

    val syncState: StateFlow<SyncState> = syncStatus.state

    val androidAuto: StateFlow<Boolean> = settings.androidAutoEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pauseSearchHistory: StateFlow<Boolean> = settings.pauseSearchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val searchHistoryCount: StateFlow<Int> = searchHistory.count
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val spatialAudio: StateFlow<SpatialState> = spatial.state

    val imageCacheEnabled: StateFlow<Boolean> = settings.imageCacheEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val localFolderUri: StateFlow<String?> = settings.localFolderUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _imageCacheSummary = MutableStateFlow(readImageCache())
    val imageCacheSummary: StateFlow<String> = _imageCacheSummary.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun checkForUpdate() = viewModelScope.launch {
        if (_updateState.value is UpdateState.Checking) return@launch
        _updateState.value = UpdateState.Checking

        updates.check(versionName)
            .onSuccess { update ->
                _updateState.value = when (update) {
                    null -> UpdateState.UpToDate
                    else -> UpdateState.Available(update)
                }
            }
            .onFailure {
                _updateState.value = UpdateState.Failed(
                    it.message ?: "Could not reach GitHub"
                )
            }
    }

    fun installUpdate() = viewModelScope.launch {
        val available = _updateState.value as? UpdateState.Available ?: return@launch
        _updateState.value = UpdateState.Downloading

        updates.downloadAndInstall(available.update)
            .onSuccess { _updateState.value = UpdateState.Available(available.update) }
            .onFailure {
                _updateState.value = UpdateState.Failed(it.message ?: "Download failed")
            }
    }

    val versionName: String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
    }.getOrNull() ?: "unknown"

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    suspend fun signIn(context: Context) {
        auth.signIn(context)
            .onSuccess { _message.value = "Signed in" }
            .onFailure { _message.value = "Sign-in failed: " + (it.message ?: it::class.simpleName) }
    }
    suspend fun signOut() = auth.signOut()

    fun setAmoled(value: Boolean) = viewModelScope.launch { settings.setAmoled(value) }
    fun setPinColours(value: Boolean) = viewModelScope.launch { settings.setPinColours(value) }
    fun setHoldThreshold(ms: Long) = viewModelScope.launch { settings.setHoldThresholdMs(ms) }

    fun setAndroidAuto(value: Boolean) = viewModelScope.launch { settings.setAndroidAuto(value) }

    fun setImageCacheEnabled(value: Boolean) = viewModelScope.launch {
        settings.setImageCacheEnabled(value)

        _message.value = if (value) "Thumbnails will be kept from the next launch"
        else "Thumbnails will stop being kept from the next launch"
    }

    fun clearImageCache() = viewModelScope.launch {
        (appContext.applicationContext as? com.abshetty.vimusic.ViMusicApplication)
            ?.artwork()?.clear()
        _imageCacheSummary.value = readImageCache()
        _message.value = "Artwork cache cleared"
    }

    private fun readImageCache(): String {
        val bytes = (appContext.applicationContext as? com.abshetty.vimusic.ViMusicApplication)
            ?.artwork()?.cacheBytes() ?: 0L
        return (bytes / (1024 * 1024)).toString() + " MB used"
    }

    fun setPauseSearchHistory(value: Boolean) = viewModelScope.launch {
        settings.setPauseSearchHistory(value)
    }

    fun setSpatialAudio(value: Boolean) = viewModelScope.launch {
        settings.setSpatialAudio(value)
        spatial.setEnabled(value)
    }

    fun exportFileName(): String {
        val who = account.value?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "library"
        return "vimusic_" + who + "_" + System.currentTimeMillis() + ".db"
    }

    fun clearSearchHistory() = viewModelScope.launch {
        searchHistory.clear()
        _message.value = "Search history cleared"
    }

    fun refreshSpatial() = spatial.refresh()

    private val _volumeSkipReady = MutableStateFlow(readVolumeSkipReady())
    val volumeSkipReady: StateFlow<Boolean> = _volumeSkipReady.asStateFlow()

    fun refreshVolumeSkip() { _volumeSkipReady.value = readVolumeSkipReady() }

    private fun readVolumeSkipReady(): Boolean = runCatching {
        val enabled = android.provider.Settings.Secure.getString(
            appContext.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        enabled.split(':').any { it.contains("VolumeKeyAccessibilityService") }
    }.getOrDefault(false)

    fun openSoundSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openUrl(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { _message.value = "Nothing on this device can open that link" }
    }

    fun refreshSync() = librarySync.syncNow()

    fun exportTo(uri: Uri) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                appContext.contentResolver.openOutputStream(uri)!!.use { backup.export(it) }
            }.getOrElse { Result.failure(it) }
        }
        _message.value = result.fold(
            onSuccess = { "Exported " + it + " rows" },
            onFailure = { "Export failed: " + (it.message ?: "unknown error") },
        )
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                appContext.contentResolver.openInputStream(uri)!!.use { backup.import(it) }
            }.getOrElse { Result.failure(it) }
        }
        _message.value = result.fold(
            onSuccess = { "Imported " + it + " rows. Syncing to the cloud." },
            onFailure = { "Import failed: " + (it.message ?: "unknown error") },
        )
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openBatterySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object Downloading : UpdateState
    data class Available(
        val update: com.abshetty.vimusic.core.data.update.Update,
    ) : UpdateState
    data class Failed(val message: String) : UpdateState
}
