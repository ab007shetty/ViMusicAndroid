package com.abshetty.vimusic.feature.player

import androidx.lifecycle.ViewModel
import com.abshetty.vimusic.core.media.audio.AudioEffects
import com.abshetty.vimusic.core.media.audio.EqualizerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val effects: AudioEffects,
) : ViewModel() {
    val state: StateFlow<EqualizerState> = effects.state

    fun setEnabled(enabled: Boolean) = effects.setEnabled(enabled)
    fun usePreset(index: Int) = effects.usePreset(index)
    fun setBand(index: Int, gainDb: Float) = effects.setBand(index, gainDb)
    fun setPreamp(db: Float) = effects.setPreamp(db)
    fun resetBands() = effects.resetBands()
    fun setBassBoost(strength: Int) = effects.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = effects.setVirtualizer(strength)
    fun setLoudness(mb: Int) = effects.setLoudness(mb)
    fun setReverb(preset: Short) = effects.setReverb(preset)
    fun setMono(mono: Boolean) = effects.setMono(mono)
    fun setBalance(balance: Float) = effects.setBalance(balance)
    fun setSpeed(speed: Float) = effects.setSpeed(speed)
    fun setPitch(pitch: Float) = effects.setPitch(pitch)
}
