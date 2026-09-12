package com.abshetty.vimusic.core.designsystem.chroma

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance as composeLuminance

object ContrastGuard {
    const val WCAG_AA_NORMAL = 4.5
    private const val MAX_STEPS = 40

    fun luminance(color: Color): Double = color.composeLuminance().toDouble()

    fun ratio(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        val lighter = maxOf(a, b)
        val darker = minOf(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun ensureReadable(
        foreground: Color,
        background: Color,
        minRatio: Double = WCAG_AA_NORMAL,
    ): Color {
        if (ratio(foreground, background) >= minRatio) return foreground

        val target = if (luminance(background) > 0.5) Color.Black else Color.White
        var best = foreground

        for (step in 1..MAX_STEPS) {
            val t = step / MAX_STEPS.toFloat()
            val candidate = Color(
                red = lerp(foreground.red, target.red, t),
                green = lerp(foreground.green, target.green, t),
                blue = lerp(foreground.blue, target.blue, t),
                alpha = foreground.alpha,
            )
            best = candidate
            if (ratio(candidate, background) >= minRatio) return candidate
        }
        return best
    }

    fun meetsAA(foreground: Color, background: Color): Boolean =
        ratio(foreground, background) >= WCAG_AA_NORMAL

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
}
