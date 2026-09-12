package com.abshetty.vimusic.core.designsystem.chroma

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

object ChromaScheme {
    val DEFAULT_SEED = Color(0xFF1DB954)

    fun build(seed: Color, dark: Boolean, amoled: Boolean): ColorScheme {
        val base = dynamicColorScheme(
            seedColor = seed,
            isDark = dark,
            isAmoled = false,
            style = PaletteStyle.Vibrant,
        )

        val scheme = if (amoled && dark) {
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
            )
        } else {
            base
        }

        return scheme.copy(
            onPrimary = ContrastGuard.ensureReadable(scheme.onPrimary, scheme.primary),
            onSecondary = ContrastGuard.ensureReadable(scheme.onSecondary, scheme.secondary),
            onTertiary = ContrastGuard.ensureReadable(scheme.onTertiary, scheme.tertiary),
            onBackground = ContrastGuard.ensureReadable(scheme.onBackground, scheme.background),
            onSurface = ContrastGuard.ensureReadable(scheme.onSurface, scheme.surface),
            onSurfaceVariant = ContrastGuard.ensureReadable(
                scheme.onSurfaceVariant, scheme.surfaceVariant
            ),
            onError = ContrastGuard.ensureReadable(scheme.onError, scheme.error),
        )
    }
}
