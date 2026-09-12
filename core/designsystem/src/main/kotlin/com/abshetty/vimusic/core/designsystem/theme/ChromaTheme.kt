package com.abshetty.vimusic.core.designsystem.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.abshetty.vimusic.core.designsystem.chroma.ChromaScheme

@Composable
fun ChromaTheme(
    seed: Color?,
    amoled: Boolean = false,
    pinned: Boolean = false,
    forceDark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = forceDark ?: isSystemInDarkTheme()
    val context = LocalContext.current

    val pinnedScheme = remember {
        if (pinned) buildScheme(seed, dark, amoled, context) else null
    }

    val target = pinnedScheme ?: buildScheme(seed, dark, amoled, context)

    MaterialTheme(
        colorScheme = target.animated(),
        typography = ChromaTypography,
        content = content,
    )
}

private fun buildScheme(
    seed: Color?,
    dark: Boolean,
    amoled: Boolean,
    context: android.content.Context,
): ColorScheme = ChromaScheme.build(seed ?: ChromaScheme.DEFAULT_SEED, dark, amoled)

@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable fun c(target: Color, label: String) =
        animateColorAsState(target, Motion.colorTransition(), label = label).value

    return copy(
        primary = c(primary, "primary"),
        onPrimary = c(onPrimary, "onPrimary"),
        primaryContainer = c(primaryContainer, "primaryContainer"),
        onPrimaryContainer = c(onPrimaryContainer, "onPrimaryContainer"),
        secondary = c(secondary, "secondary"),
        onSecondary = c(onSecondary, "onSecondary"),
        secondaryContainer = c(secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = c(onSecondaryContainer, "onSecondaryContainer"),
        tertiary = c(tertiary, "tertiary"),
        background = c(background, "background"),
        onBackground = c(onBackground, "onBackground"),
        surface = c(surface, "surface"),
        onSurface = c(onSurface, "onSurface"),
        surfaceVariant = c(surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = c(onSurfaceVariant, "onSurfaceVariant"),
        surfaceContainer = c(surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = c(surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = c(surfaceContainerHighest, "surfaceContainerHighest"),
        outline = c(outline, "outline"),
    )
}
