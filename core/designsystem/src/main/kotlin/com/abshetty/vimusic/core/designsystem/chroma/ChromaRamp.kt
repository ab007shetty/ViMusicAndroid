package com.abshetty.vimusic.core.designsystem.chroma

import androidx.compose.ui.graphics.Color
import com.abshetty.vimusic.core.designsystem.vimusic.ColorPalette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

object ChromaRamp {
    fun tint(fixed: ColorPalette, seed: Color, dark: Boolean, amoled: Boolean): ColorPalette {
        val scheme = runCatching {
            dynamicColorScheme(
                seedColor = seed,
                isDark = dark,
                isAmoled = false,
                style = PaletteStyle.Vibrant,
            )
        }.getOrNull() ?: return fixed

        val background0 = if (amoled && dark) Color.Black else scheme.surfaceContainerLowest
        val background1 = if (amoled && dark) Color.Black else scheme.surfaceContainer
        val background2 = scheme.surfaceContainerHigh

        val text = ContrastGuard.ensureReadable(scheme.onSurface, background1)
        val textSecondary = ContrastGuard.ensureReadable(scheme.onSurfaceVariant, background1)

        return fixed.copy(
            background0 = background0,
            background1 = background1,
            background2 = background2,
            text = text,
            textSecondary = textSecondary,

            textDisabled = textSecondary.copy(alpha = 0.55f).compositeOverOpaque(background1),
            onAccent = scheme.onPrimary,
        )
    }

    private fun Color.compositeOverOpaque(background: Color): Color = Color(
        red = red * alpha + background.red * (1 - alpha),
        green = green * alpha + background.green * (1 - alpha),
        blue = blue * alpha + background.blue * (1 - alpha),
        alpha = 1f,
    )
}
