package com.abshetty.vimusic.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("vimusic_settings")

@Singleton

class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val AMOLED = booleanPreferencesKey("amoled")
        val PIN_COLOURS = booleanPreferencesKey("pin_colours")
        val HOLD_THRESHOLD = longPreferencesKey("volume_hold_threshold_ms")
        val ANDROID_AUTO = booleanPreferencesKey("android_auto_enabled")
        val PAUSE_SEARCH_HISTORY = booleanPreferencesKey("pause_search_history")
        val SPATIAL_AUDIO = booleanPreferencesKey("spatial_audio")
        val IMAGE_CACHE_ENABLED = booleanPreferencesKey("image_cache_enabled")
        val SETUP_DONE = booleanPreferencesKey("setup_complete")
        val LOCAL_FOLDER = stringPreferencesKey("local_folder_uri")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_GAINS = stringPreferencesKey("eq_gains")
        val EQ_PREAMP = floatPreferencesKey("eq_preamp")
        val EQ_BASS = intPreferencesKey("eq_bass")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val EQ_LOUDNESS = intPreferencesKey("eq_loudness")
        val EQ_REVERB = intPreferencesKey("eq_reverb")
        val EQ_MONO = booleanPreferencesKey("eq_mono")
        val EQ_BALANCE = floatPreferencesKey("eq_balance")
    }

    data class EqualizerPrefs(
        val enabled: Boolean = false,
        val gainsDb: List<Float> = emptyList(),
        val preampDb: Float = 0f,
        val bassBoost: Int = 0,
        val virtualizer: Int = 0,
        val loudnessMb: Int = 0,
        val reverb: Short = 0,
        val mono: Boolean = false,
        val balance: Float = 0f,
    )

    val amoled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AMOLED] ?: false }

    val pinColours: Flow<Boolean> = context.dataStore.data.map { it[Keys.PIN_COLOURS] ?: false }

    val holdThresholdMs: Flow<Long> =
        context.dataStore.data.map { it[Keys.HOLD_THRESHOLD] ?: 450L }

    val androidAutoEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ANDROID_AUTO] ?: false }

    val pauseSearchHistory: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.PAUSE_SEARCH_HISTORY] ?: false }

    val spatialAudio: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SPATIAL_AUDIO] ?: true }

    val setupComplete: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SETUP_DONE] ?: false }

    val localFolderUri: Flow<String?> =
        context.dataStore.data.map { it[Keys.LOCAL_FOLDER] }

    val equalizer: Flow<EqualizerPrefs> = context.dataStore.data.map { prefs ->
        EqualizerPrefs(
            enabled = prefs[Keys.EQ_ENABLED] ?: false,
            gainsDb = prefs[Keys.EQ_GAINS]
                ?.split(',')
                ?.mapNotNull { it.toFloatOrNull() }
                ?: emptyList(),
            preampDb = prefs[Keys.EQ_PREAMP] ?: 0f,
            bassBoost = prefs[Keys.EQ_BASS] ?: 0,
            virtualizer = prefs[Keys.EQ_VIRTUALIZER] ?: 0,
            loudnessMb = prefs[Keys.EQ_LOUDNESS] ?: 0,
            reverb = (prefs[Keys.EQ_REVERB] ?: 0).toShort(),
            mono = prefs[Keys.EQ_MONO] ?: false,
            balance = prefs[Keys.EQ_BALANCE] ?: 0f,
        )
    }

    val imageCacheEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.IMAGE_CACHE_ENABLED] ?: true }

    suspend fun setAmoled(value: Boolean) {
        context.dataStore.edit { it[Keys.AMOLED] = value }
    }

    suspend fun setPinColours(value: Boolean) {
        context.dataStore.edit { it[Keys.PIN_COLOURS] = value }
    }

    suspend fun setSetupComplete(value: Boolean) {
        context.dataStore.edit { it[Keys.SETUP_DONE] = value }
    }

    suspend fun setLocalFolderUri(value: String?) {
        context.dataStore.edit {
            if (value == null) it.remove(Keys.LOCAL_FOLDER) else it[Keys.LOCAL_FOLDER] = value
        }
    }

    suspend fun setEqualizer(prefs: EqualizerPrefs) {
        context.dataStore.edit {
            it[Keys.EQ_ENABLED] = prefs.enabled
            it[Keys.EQ_GAINS] = prefs.gainsDb.joinToString(",")
            it[Keys.EQ_PREAMP] = prefs.preampDb
            it[Keys.EQ_BASS] = prefs.bassBoost
            it[Keys.EQ_VIRTUALIZER] = prefs.virtualizer
            it[Keys.EQ_LOUDNESS] = prefs.loudnessMb
            it[Keys.EQ_REVERB] = prefs.reverb.toInt()
            it[Keys.EQ_MONO] = prefs.mono
            it[Keys.EQ_BALANCE] = prefs.balance
        }
    }

    suspend fun setImageCacheEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.IMAGE_CACHE_ENABLED] = value }
    }

    suspend fun setAndroidAuto(value: Boolean) {
        context.dataStore.edit { it[Keys.ANDROID_AUTO] = value }
    }

    suspend fun setPauseSearchHistory(value: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_SEARCH_HISTORY] = value }
    }

    suspend fun setSpatialAudio(value: Boolean) {
        context.dataStore.edit { it[Keys.SPATIAL_AUDIO] = value }
    }

    suspend fun setHoldThresholdMs(value: Long) {
        context.dataStore.edit { it[Keys.HOLD_THRESHOLD] = value.coerceIn(250L, 1200L) }
    }
}
