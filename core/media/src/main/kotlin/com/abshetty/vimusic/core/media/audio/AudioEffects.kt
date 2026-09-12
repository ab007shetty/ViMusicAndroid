package com.abshetty.vimusic.core.media.audio

import android.media.audiofx.PresetReverb
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.abshetty.vimusic.core.datastore.SettingsStore
import com.abshetty.vimusic.core.media.audio.dsp.EqualizerAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class Band(
    val index: Int,
    val centreHz: Int,
    val gainDb: Float,
) {
    val label: String
        get() = if (centreHz >= 1000) (centreHz / 1000).toString() + "k" else centreHz.toString()
}

data class EqualizerState(
    val enabled: Boolean = false,
    val bands: List<Band> = emptyList(),
    val minGainDb: Float = EqualizerAudioProcessor.MIN_GAIN_DB,
    val maxGainDb: Float = EqualizerAudioProcessor.MAX_GAIN_DB,
    val presets: List<String> = EqPreset.labels,

    val currentPreset: Int = 0,
    val preampDb: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessMb: Int = 0,

    val hasReverb: Boolean = false,
    val reverb: Short = PresetReverb.PRESET_NONE,

    val mono: Boolean = false,

    val balance: Float = 0f,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
) {
    val isActive: Boolean
        get() = enabled && (
            bands.any { it.gainDb != 0f } || preampDb != 0f ||
                bassBoost > 0 || virtualizer > 0 || loudnessMb > 0 ||
                reverb != PresetReverb.PRESET_NONE || mono || balance != 0f
            )

    val presetLabel: String
        get() = EqPreset.entries.getOrNull(currentPreset)?.label ?: "Custom"
}

@OptIn(UnstableApi::class)
@Singleton
class AudioEffects @Inject constructor(
    private val processor: EqualizerAudioProcessor,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private var reverbEffect: PresetReverb? = null
    private var sessionId: Int = 0

    private val _state = MutableStateFlow(
        EqualizerState(
            bands = EqualizerAudioProcessor.CENTRES_HZ.mapIndexed { i, hz -> Band(i, hz, 0f) },
        )
    )
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    init {
        scope.launch { runCatching { restore() } }
    }

    private suspend fun restore() {
        val saved = settings.equalizer.first()
        val gains = FloatArray(EqualizerAudioProcessor.BAND_COUNT) {
            saved.gainsDb.getOrElse(it) { 0f }
        }
        _state.value = _state.value.copy(
            enabled = saved.enabled,
            bands = bandsFrom(gains),
            currentPreset = EqPreset.matching(gains)?.ordinal ?: -1,
            preampDb = saved.preampDb,
            bassBoost = saved.bassBoost,
            virtualizer = saved.virtualizer,
            loudnessMb = saved.loudnessMb,
            reverb = saved.reverb,
            mono = saved.mono,
            balance = saved.balance,
        )
        pushToProcessor()
    }

    private fun bandsFrom(gains: FloatArray): List<Band> =
        EqualizerAudioProcessor.CENTRES_HZ.mapIndexed { i, hz -> Band(i, hz, gains[i]) }

    private fun gainsOf(state: EqualizerState) =
        FloatArray(EqualizerAudioProcessor.BAND_COUNT) { i ->
            state.bands.getOrNull(i)?.gainDb ?: 0f
        }

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (audioSessionId == sessionId && reverbEffect != null) return

        releaseReverb()
        sessionId = audioSessionId
        reverbEffect = runCatching { PresetReverb(EFFECT_PRIORITY, audioSessionId) }
            .onFailure { Log.w(TAG, "PresetReverb unavailable: " + it.message) }
            .getOrNull()

        _state.value = _state.value.copy(hasReverb = reverbEffect != null)
        applyReverb(_state.value.reverb, _state.value.enabled)
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        pushToProcessor()
        applyReverb(_state.value.reverb, enabled)
        persist()
    }

    fun setBand(index: Int, gainDb: Float) {
        val clamped = gainDb.coerceIn(state.value.minGainDb, state.value.maxGainDb)
        val bands = _state.value.bands.map {
            if (it.index == index) it.copy(gainDb = clamped) else it
        }
        _state.value = _state.value.copy(
            bands = bands,

            currentPreset = EqPreset.matching(
                FloatArray(bands.size) { bands[it].gainDb }
            )?.ordinal ?: -1,

            enabled = true,
        )
        pushToProcessor()
        persist()
    }

    fun usePreset(index: Int) {
        val preset = EqPreset.entries.getOrNull(index) ?: return
        _state.value = _state.value.copy(
            bands = bandsFrom(preset.gains.copyOf()),
            currentPreset = index,
            enabled = true,
        )
        pushToProcessor()
        persist()
    }

    fun resetBands() = usePreset(EqPreset.FLAT.ordinal)

    fun setPreamp(db: Float) {
        _state.value = _state.value.copy(preampDb = db.coerceIn(-12f, 12f))
        pushToProcessor()
        persist()
    }

    fun setBassBoost(strength: Int) {
        _state.value = _state.value.copy(bassBoost = strength.coerceIn(0, 1000), enabled = true)
        pushToProcessor()
        persist()
    }

    fun setVirtualizer(strength: Int) {
        _state.value = _state.value.copy(virtualizer = strength.coerceIn(0, 1000), enabled = true)
        pushToProcessor()
        persist()
    }

    fun setLoudness(millibels: Int) {
        _state.value = _state.value.copy(loudnessMb = millibels.coerceIn(0, 1200), enabled = true)
        pushToProcessor()
        persist()
    }

    fun setMono(mono: Boolean) {
        _state.value = _state.value.copy(mono = mono, enabled = if (mono) true else _state.value.enabled)
        pushToProcessor()
        persist()
    }

    fun setBalance(balance: Float) {
        _state.value = _state.value.copy(balance = balance.coerceIn(-1f, 1f))
        if (balance != 0f) _state.value = _state.value.copy(enabled = true)
        pushToProcessor()
        persist()
    }

    fun setReverb(preset: Short) {
        _state.value = _state.value.copy(reverb = preset)

        if (preset != PresetReverb.PRESET_NONE && !_state.value.enabled) {
            setEnabled(true)
        } else {
            applyReverb(preset, _state.value.enabled)
            persist()
        }
    }

    fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(speed = speed.coerceIn(0.25f, 3.0f))
    }

    fun setPitch(pitch: Float) {
        _state.value = _state.value.copy(pitch = pitch.coerceIn(0.5f, 2.0f))
    }

    private fun pushToProcessor() {
        val s = _state.value
        processor.setGains(gainsOf(s))
        processor.setPreamp(s.preampDb)
        processor.setBassBoost(s.bassBoost / 1000f)
        processor.setWidth(s.virtualizer / 1000f)
        processor.setMakeup(s.loudnessMb / 100f)
        processor.setMono(s.mono)
        processor.setBalance(s.balance)
        processor.setEnabled(s.enabled)
    }

    private fun applyReverb(preset: Short, enabled: Boolean) {
        runCatching {
            reverbEffect?.preset = preset
            reverbEffect?.enabled = enabled && preset != PresetReverb.PRESET_NONE
        }
    }

    private fun persist() {
        val s = _state.value
        scope.launch {
            runCatching {
                settings.setEqualizer(
                    SettingsStore.EqualizerPrefs(
                        enabled = s.enabled,
                        gainsDb = gainsOf(s).toList(),
                        preampDb = s.preampDb,
                        bassBoost = s.bassBoost,
                        virtualizer = s.virtualizer,
                        loudnessMb = s.loudnessMb,
                        reverb = s.reverb,
                        mono = s.mono,
                        balance = s.balance,
                    )
                )
            }
        }
    }

    private fun releaseReverb() {
        runCatching { reverbEffect?.release() }
        reverbEffect = null
    }

    fun release() {
        releaseReverb()
        sessionId = 0
    }

    private companion object {
        const val TAG = "ViMusicAudioFx"

        const val EFFECT_PRIORITY = 100
    }
}

enum class ReverbPreset(val value: Short, val label: String) {
    NONE(PresetReverb.PRESET_NONE, "Off"),
    SMALL_ROOM(PresetReverb.PRESET_SMALLROOM, "Small room"),
    MEDIUM_ROOM(PresetReverb.PRESET_MEDIUMROOM, "Medium room"),
    LARGE_ROOM(PresetReverb.PRESET_LARGEROOM, "Large room"),
    MEDIUM_HALL(PresetReverb.PRESET_MEDIUMHALL, "Medium hall"),
    LARGE_HALL(PresetReverb.PRESET_LARGEHALL, "Large hall"),
    PLATE(PresetReverb.PRESET_PLATE, "Plate"),
}
