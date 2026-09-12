package com.abshetty.vimusic.core.media.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    private var s1 = DoubleArray(0)
    private var s2 = DoubleArray(0)

    var bypass: Boolean = true
        private set

    fun prepare(channelCount: Int) {
        if (s1.size != channelCount) {
            s1 = DoubleArray(channelCount)
            s2 = DoubleArray(channelCount)
        }
    }

    fun reset() {
        s1.fill(0.0)
        s2.fill(0.0)
    }

    fun setPeaking(sampleRate: Int, centreHz: Double, q: Double, gainDb: Double) {
        if (gainDb == 0.0) { setIdentity(); return }
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * centreHz / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        set(
            b0 = 1 + alpha * a,
            b1 = -2 * cos(w0),
            b2 = 1 - alpha * a,
            a0 = 1 + alpha / a,
            a1 = -2 * cos(w0),
            a2 = 1 - alpha / a,
        )
    }

    fun setLowShelf(sampleRate: Int, cornerHz: Double, gainDb: Double) {
        if (gainDb == 0.0) { setIdentity(); return }
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * cornerHz / sampleRate
        val cosW = cos(w0)

        val alpha = sin(w0) / 2.0 * sqrt(2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
        set(
            b0 = a * ((a + 1) - (a - 1) * cosW + twoSqrtAAlpha),
            b1 = 2 * a * ((a - 1) - (a + 1) * cosW),
            b2 = a * ((a + 1) - (a - 1) * cosW - twoSqrtAAlpha),
            a0 = (a + 1) + (a - 1) * cosW + twoSqrtAAlpha,
            a1 = -2 * ((a - 1) + (a + 1) * cosW),
            a2 = (a + 1) + (a - 1) * cosW - twoSqrtAAlpha,
        )
    }

    fun setHighShelf(sampleRate: Int, cornerHz: Double, gainDb: Double) {
        if (gainDb == 0.0) { setIdentity(); return }
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * cornerHz / sampleRate
        val cosW = cos(w0)
        val alpha = sin(w0) / 2.0 * sqrt(2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
        set(
            b0 = a * ((a + 1) + (a - 1) * cosW + twoSqrtAAlpha),
            b1 = -2 * a * ((a - 1) + (a + 1) * cosW),
            b2 = a * ((a + 1) + (a - 1) * cosW - twoSqrtAAlpha),
            a0 = (a + 1) - (a - 1) * cosW + twoSqrtAAlpha,
            a1 = 2 * ((a - 1) - (a + 1) * cosW),
            a2 = (a + 1) - (a - 1) * cosW - twoSqrtAAlpha,
        )
    }

    private fun setIdentity() {
        b0 = 1.0; b1 = 0.0; b2 = 0.0; a1 = 0.0; a2 = 0.0
        bypass = true
    }

    private fun set(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) {
        this.b0 = b0 / a0
        this.b1 = b1 / a0
        this.b2 = b2 / a0
        this.a1 = a1 / a0
        this.a2 = a2 / a0
        bypass = false
    }

    fun process(channel: Int, x: Double): Double {
        val y = b0 * x + s1[channel]
        s1[channel] = b1 * x - a1 * y + s2[channel]
        s2[channel] = b2 * x - a2 * y
        return y
    }
}
