package com.abshetty.vimusic.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.feature.library.LibraryScreen
import com.abshetty.vimusic.feature.player.EqualizerScreen
import com.abshetty.vimusic.feature.player.FullPlayer
import com.abshetty.vimusic.feature.player.MiniPlayer
import com.abshetty.vimusic.feature.search.SearchScreen
import com.abshetty.vimusic.settings.SettingsScreen
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

@Composable
fun ViMusicApp(sharedLink: String? = null) {
    var overlay by rememberSaveable { mutableStateOf<Destination?>(null) }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }

    var searchFocusTrigger by rememberSaveable { mutableIntStateOf(0) }

    val startQuery = remember(sharedLink) {
        sharedLink?.also { overlay = Destination.SEARCH }
    }

    BackHandler(enabled = playerExpanded) { playerExpanded = false }
    BackHandler(enabled = !playerExpanded && overlay != null) { overlay = null }

    val (colorPalette) = LocalAppearance.current

    Box(
        Modifier
            .fillMaxSize()
            .background(colorPalette.background0)

            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                LibraryScreen(
                    onOpenPlayer = { playerExpanded = true },
                    onOpenSearch = {
                        if (overlay == Destination.SEARCH) searchFocusTrigger++
                        overlay = Destination.SEARCH
                    },
                    onOpenEqualizer = { overlay = Destination.EQUALIZER },
                    onOpenSettings = { overlay = Destination.SETTINGS },
                )

                overlay?.let { current ->
                    Box(Modifier.fillMaxSize().background(colorPalette.background0)) {
                        when (current) {
                            Destination.SEARCH -> SearchScreen(
                                onOpenPlayer = { playerExpanded = true },
                                initialQuery = startQuery,
                                focusTrigger = searchFocusTrigger,
                                onBack = { overlay = null },
                            )
                            Destination.EQUALIZER -> EqualizerScreen(
                                onBack = { overlay = null },
                            )
                            Destination.SETTINGS -> SettingsScreen(
                                onBack = { overlay = null },
                                onOpenEqualizer = { overlay = Destination.EQUALIZER },
                            )
                            Destination.LIBRARY -> Unit
                        }
                    }
                }
            }

            MiniPlayer(onExpand = { playerExpanded = true })
        }

        val expansion = remember { Animatable(0f) }
        LaunchedEffect(playerExpanded) {
            if (playerExpanded) {
                expansion.animateTo(1f, playerSpring())
            } else if (expansion.value > 0f) {
                expansion.animateTo(0f, playerSpring())
            }
        }

        if (playerExpanded || expansion.value > 0.001f) {
            FullPlayer(

                onOpenEqualizer = {
                    playerExpanded = false
                    overlay = Destination.EQUALIZER
                },
                expansion = expansion,
                onCollapse = { playerExpanded = false },

                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun playerSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = 0.001f,
)
