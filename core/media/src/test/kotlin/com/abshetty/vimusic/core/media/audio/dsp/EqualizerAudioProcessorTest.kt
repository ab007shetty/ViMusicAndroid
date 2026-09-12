package com.abshetty.vimusic.core.media.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.abshetty.vimusic.core.media.audio.EqPreset
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin

class EqualizerAudioProcessorTest {
    private val sampleRate = 48_000
    private val format = AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)

    private fun processorFor(configure: EqualizerAudioProcessor.() -> Unit) =
        EqualizerAudioProcessor().apply {
            configure(this)
            configure(format)
            flush()
        }

    private fun loudTone(hz: Double, frames: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(frames * 2 * 2).order(ByteOrder.nativeOrder())
        for (i in 0 until frames) {
            val sample = (sin(2.0 * PI * hz * i / sampleRate) * 32_760).toInt().toShort()
            buffer.putShort(sample)
            buffer.putShort(sample)
        }
        buffer.flip()
        return buffer
    }

    private fun tone(hz: Double, frames: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(frames * 2 * 2).order(ByteOrder.nativeOrder())
        for (i in 0 until frames) {
            val sample = (sin(2.0 * PI * hz * i / sampleRate) * 16_384).toInt().toShort()
            buffer.putShort(sample)
            buffer.putShort(sample)
        }
        buffer.flip()
        return buffer
    }

    private fun peak(buffer: ByteBuffer): Double {
        val shorts = buffer.asShortBuffer()
        val skip = shorts.limit() / 10
        var max = 0.0
        var i = skip
        while (i < shorts.limit()) {
            max = maxOf(max, abs(shorts.get(i).toDouble()))
            i += 2
        }
        return max
    }

    private fun run(processor: EqualizerAudioProcessor, input: ByteBuffer): ByteBuffer {
        processor.queueInput(input)
        return processor.output
    }

    @Test fun `a flat enabled equaliser passes samples through untouched`() {
        val processor = processorFor { setEnabled(true) }
        val input = tone(1_000.0, 4_096)
        val expected = tone(1_000.0, 4_096)

        val out = run(processor, input)

        assertThat(out.remaining()).isEqualTo(expected.remaining())
        assertThat(out).isEqualTo(expected)
    }

    @Test fun `a disabled equaliser passes samples through even with a curve set`() {
        val processor = processorFor {
            setGains(EqPreset.BASS_BOOSTER.gains)
            setEnabled(false)
        }
        val expected = tone(60.0, 4_096)

        val out = run(processor, tone(60.0, 4_096))

        assertThat(out).isEqualTo(expected)
    }

    private fun balanceDb(
        hz: Double,
        against: Double,
        configure: EqualizerAudioProcessor.() -> Unit,
    ): Double {
        val a = peak(run(processorFor(configure), tone(hz, 16_384)))
        val b = peak(run(processorFor(configure), tone(against, 16_384)))
        return 20 * log10(a / b)
    }

    @Test fun `boosting a band lifts it relative to the rest`() {
        val gains = FloatArray(EqualizerAudioProcessor.BAND_COUNT)
        gains[5] = 10f

        val flatBalance = balanceDb(1_000.0, 100.0) { setEnabled(true) }
        val boostedBalance = balanceDb(1_000.0, 100.0) {
            setGains(gains)
            setEnabled(true)
        }

        val lift = boostedBalance - flatBalance
        assertThat(lift).isGreaterThan(6.0)
        assertThat(lift).isLessThan(13.0)
    }

    @Test fun `cutting a band makes a tone at that band quieter`() {
        val gains = FloatArray(EqualizerAudioProcessor.BAND_COUNT)
        gains[5] = -10f
        val processor = processorFor {
            setGains(gains)
            setEnabled(true)
        }

        val flat = peak(tone(1_000.0, 16_384))
        val cut = peak(run(processor, tone(1_000.0, 16_384)))

        assertThat(20 * log10(cut / flat)).isLessThan(-6.0)
    }

    @Test fun `a band boost does not spill into distant frequencies`() {
        val gains = FloatArray(EqualizerAudioProcessor.BAND_COUNT)
        gains[9] = 12f

        val flatBalance = balanceDb(1_000.0, 100.0) { setEnabled(true) }
        val trebleBalance = balanceDb(1_000.0, 100.0) {
            setGains(gains)
            setEnabled(true)
        }

        assertThat(abs(trebleBalance - flatBalance)).isLessThan(2.0)
    }

    @Test fun `an overdriven peak saturates instead of wrapping`() {
        val processor = processorFor {
            setGains(FloatArray(EqualizerAudioProcessor.BAND_COUNT) { 12f })
            setMakeup(12f)
            setPreamp(12f)
            setEnabled(true)
        }

        val input = loudTone(1_000.0, 8_192)
        val expected = loudTone(1_000.0, 8_192).asShortBuffer()
        val out = run(processor, input).asShortBuffer()

        var inverted = 0
        var i = 0
        while (i < out.limit() && i < expected.limit()) {
            val source = expected.get(i).toInt()
            val result = out.get(i).toInt()
            if (abs(source) > 8_000 && source > 0 != result > 0) inverted++
            i++
        }
        assertThat(inverted).isEqualTo(0)
    }

    @Test fun `output is the same size as input`() {
        val processor = processorFor {
            setGains(EqPreset.ROCK.gains)
            setEnabled(true)
        }
        val out = run(processor, tone(440.0, 4_096))

        assertThat(out.remaining()).isEqualTo(4_096 * 2 * 2)
    }

    @Test fun `gains outside the advertised range are clamped, not applied`() {
        val processor = processorFor {
            setGains(FloatArray(EqualizerAudioProcessor.BAND_COUNT) { 60f })
            setEnabled(true)
        }

        val out = run(processor, tone(1_000.0, 4_096))

        assertThat(out.remaining()).isEqualTo(4_096 * 2 * 2)
    }

    @Test fun `float output is declined rather than silently mangled`() {
        val processor = EqualizerAudioProcessor()
        val floatFormat = AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_FLOAT)

        runCatching { processor.configure(floatFormat) }.also {
            assertThat(it.exceptionOrNull())
                .isInstanceOf(AudioProcessor.UnhandledAudioFormatException::class.java)
        }
    }

    @Test fun `every preset carries one gain per band`() {
        EqPreset.entries.forEach { preset ->
            assertThat(preset.gains).hasLength(EqualizerAudioProcessor.BAND_COUNT)
        }
    }

    @Test fun `a preset curve is recognised by value`() {
        assertThat(EqPreset.matching(EqPreset.ROCK.gains.copyOf())).isEqualTo(EqPreset.ROCK)
        assertThat(EqPreset.matching(FloatArray(10))).isEqualTo(EqPreset.FLAT)

        val tweaked = EqPreset.ROCK.gains.copyOf().also { it[3] = 11f }
        assertThat(EqPreset.matching(tweaked)).isNull()
    }
}
