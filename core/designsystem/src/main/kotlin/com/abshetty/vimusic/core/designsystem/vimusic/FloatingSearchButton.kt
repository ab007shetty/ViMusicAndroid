package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.FloatingActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollableState? = null,
) {
    val (colorPalette) = LocalAppearance.current

    val visible by remember(scrollState) {
        derivedStateOf { scrollState?.isScrollInProgress != true }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colorPalette.background2)
                .clickable(onClick = onClick)
                .padding(16.dp),
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = colorPalette.text,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
