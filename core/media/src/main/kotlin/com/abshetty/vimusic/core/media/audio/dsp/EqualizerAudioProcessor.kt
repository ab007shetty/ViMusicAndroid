package com.abshetty.vimusic.core.media.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tanh

@UnstableApi
class EqualizerAudioProcessor : BaseAudioProcessor() {
    private class Settings(
        val gainsDb: FloatArray = FloatArray(BAND_COUNT),
        val preampDb: Float = 0f,
        val bassBoost: Float = 0f,
        val width: Float = 0f,
        val makeupDb: Float = 0f,
        val mono: Boolean = false,
        val balance: Float = 0f,
        val enabled: Boolean = false,
    ) {
        fun copy(
            gainsDb: FloatArray = this.gainsDb,
            preampDb: Float = this.preampDb,
            bassBoost: Float = this.bassBoost,
            width: Float = this.width,
            makeupDb: Float = this.makeupDb,
            mono: Boolean = this.mono,
            balance: Float = this.balance,
            enabled: Boolean = this.enabled,
        ) = Settings(gainsDb, preampDb, bassBoost, width, makeupDb, mono, balance, enabled)

        val transparent: Boolean
            get() = !enabled || (
                gainsDb.all { it == 0f } && preampDb == 0f &&
                    bassBoost == 0f && width == 0f && makeupDb == 0f &&
                    !mono && balance == 0f
                )
    }

    @Volatile private var settings = Settings()

    private val bands = Array(BAND_COUNT) { Biquad() }
    private val bassShelf = Biquad()
    private var appliedTo: Settings? = null
    private var sampleRate = 0
    private var channelCount = 0

    private var linearGain = 1.0
    private var sideGain = 1.0
    private var leftGain = 1.0
    private var rightGain = 1.0

    private var scratch = DoubleArray(2)

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setGains(gainsDb: FloatArray) = update { current ->
        val next = FloatArray(BAND_COUNT) {
            gainsDb.getOrElse(it) { 0f }.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        }
        current.copy(gainsDb = next)
    }

    fun setPreamp(db: Float) = update { it.copy(preampDb = db.coerceIn(-12f, 12f)) }

    fun setBassBoost(amount: Float) = update { it.copy(bassBoost = amount.coerceIn(0f, 1f)) }

    fun setWidth(amount: Float) = update { it.copy(width = amount.coerceIn(0f, 1f)) }

    fun setMakeup(db: Float) = update { it.copy(makeupDb = db.coerceIn(0f, 12f)) }

    fun setMono(mono: Boolean) = update { it.copy(mono = mono) }

    fun setBalance(balance: Float) = update { it.copy(balance = balance.coerceIn(-1f, 1f)) }

    private inline fun update(block: (Settings) -> Settings) {
        settings = block(settings)
    }

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        if (scratch.size != channelCount) scratch = DoubleArray(channelCount)
        appliedTo = null
        return inputAudioFormat
    }

    override fun isActive(): Boolean = sampleRate != 0

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val current = settings
        val output = replaceOutputBuffer(remaining)

        if (current.transparent) {
            output.put(inputBuffer)
            output.flip()
            return
        }

        if (appliedTo !== current) rebuild(current)

        val src = inputBuffer.asShortBuffer()
        val dst = output.asShortBuffer()

        val channels = channelCount
        val frames = remaining / 2 / channels
        val stereo = channels == 2
        val widen = stereo && sideGain != 1.0 && !current.mono
        val toMono = stereo && current.mono
        val leftGain = this.leftGain
        val rightGain = this.rightGain
        val panned = stereo && (leftGain != 1.0 || rightGain != 1.0)
        val frame = scratch

        for (f in 0 until frames) {
            for (ch in 0 until channels) {
                var x = src.get().toDouble() / SHORT_SCALE
                if (!bassShelf.bypass) x = bassShelf.process(ch, x)
                for (b in 0 until BAND_COUNT) {
                    val band = bands[b]
                    if (!band.bypass) x = band.process(ch, x)
                }
                frame[ch] = x * linearGain
            }

            if (toMono) {
                val mid = (frame[0] + frame[1]) * 0.5
                frame[0] = mid
                frame[1] = mid
            } else if (widen) {
                val mid = (frame[0] + frame[1]) * 0.5
                val side = (frame[0] - frame[1]) * 0.5 * sideGain
                frame[0] = mid + side
                frame[1] = mid - side
            }

            if (panned) {
                frame[0] *= leftGain
                frame[1] *= rightGain
            }

            for (ch in 0 until channels) {
                val scaled = (softClip(frame[ch]) * SHORT_SCALE).roundToInt()
                dst.put(scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            }
        }

        inputBuffer.position(inputBuffer.position() + frames * channels * 2)
        output.position(frames * channels * 2)
        while (inputBuffer.hasRemaining()) output.put(inputBuffer.get())

        output.flip()
    }

    private fun softClip(x: Double): Double {
        if (x <= SOFT_KNEE && x >= -SOFT_KNEE) return x
        val sign = if (x < 0) -1.0 else 1.0
        val over = (abs(x) - SOFT_KNEE) / (1.0 - SOFT_KNEE)
        return sign * (SOFT_KNEE + (1.0 - SOFT_KNEE) * tanh(over))
    }

    override fun onFlush() {
        bands.forEach { it.reset() }
        bassShelf.reset()
    }

    override fun onReset() {
        sampleRate = 0
        channelCount = 0
        appliedTo = null
    }

    private fun rebuild(s: Settings) {
        val rate = sampleRate
        bands.forEachIndexed { i, biquad ->
            biquad.prepare(channelCount)
            val gain = s.gainsDb[i].toDouble()
            val centre = CENTRES_HZ[i].toDouble()
            when (i) {
                0 -> biquad.setLowShelf(rate, centre, gain)
                BAND_COUNT - 1 -> biquad.setHighShelf(rate, centre, gain)

                else -> biquad.setPeaking(rate, centre, BAND_Q, gain)
            }
        }

        bassShelf.prepare(channelCount)
        bassShelf.setLowShelf(rate, BASS_CORNER_HZ, s.bassBoost * MAX_BASS_DB)

        val bandBoostDb = max(0f, s.gainsDb.max()).toDouble()
        val bassDb = s.bassBoost * MAX_BASS_DB
        val widthDb = 20.0 * log10(1.0 + s.width)
        val headroom = -(bandBoostDb + bassDb * 0.8 + widthDb * 0.5)

        linearGain = 10.0.pow((s.preampDb + s.makeupDb + headroom) / 20.0)
        sideGain = 1.0 + s.width

        leftGain = if (s.balance > 0f) (1.0 - s.balance).coerceAtLeast(0.0) else 1.0
        rightGain = if (s.balance < 0f) (1.0 + s.balance).coerceAtLeast(0.0) else 1.0

        appliedTo = s
    }

    companion object {
        const val BAND_COUNT = 10
        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f

        val CENTRES_HZ = intArrayOf(31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)

        private const val BAND_Q = 1.41
        private const val BASS_CORNER_HZ = 90.0
        private const val MAX_BASS_DB = 12.0
        private const val SHORT_SCALE = 32768.0

        private const val SOFT_KNEE = 0.9
    }
}
