package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.abshetty.vimusic.core.designsystem.chroma.ChromaRamp
import com.abshetty.vimusic.core.designsystem.chroma.ContrastGuard
import com.abshetty.vimusic.core.designsystem.theme.Motion

@Composable
fun ViMusicTheme(
    seed: Color?,
    amoled: Boolean = false,

    pinColours: Boolean = false,
    forceDark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = forceDark ?: isSystemInDarkTheme()

    val fixed = when {
        !dark -> DefaultLightPalette
        amoled -> PureBlackPalette
        else -> DefaultDarkPalette
    }

    val activeSeed = seed.takeUnless { pinColours }

    val base = if (activeSeed == null) {
        fixed
    } else {
        ChromaRamp.tint(fixed, activeSeed, dark = dark, amoled = amoled)
    }

    val rawAccent = activeSeed ?: DefaultAccent
    val accent = ContrastGuard.ensureReadable(rawAccent, base.background1)

    val transition = Motion.colorTransition<Color>()
    val animated = animateColorAsState(accent, transition, label = "accent").value
    val palette = base.copy(
        accent = animated,
        background0 = animateColorAsState(base.background0, transition, label = "bg0").value,
        background1 = animateColorAsState(base.background1, transition, label = "bg1").value,
        background2 = animateColorAsState(base.background2, transition, label = "bg2").value,
    )
    val appearance = Appearance(
        colorPalette = palette,
        typography = typographyOf(palette.text),
        thumbnailShape = RoundedCornerShape(ThumbnailRadius),
    )

    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background0,
            onBackground = palette.text,
            surface = palette.background1,
            onSurface = palette.text,
            surfaceVariant = palette.background2,
            onSurfaceVariant = palette.textSecondary,
            error = palette.red,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background0,
            onBackground = palette.text,
            surface = palette.background1,
            onSurface = palette.text,
            surfaceVariant = palette.background2,
            onSurfaceVariant = palette.textSecondary,
            error = palette.red,
        )
    }

    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

val DefaultAccent = Color(0xFF1DB954)
