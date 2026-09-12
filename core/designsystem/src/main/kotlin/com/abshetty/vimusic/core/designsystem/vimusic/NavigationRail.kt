package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

@Composable
fun NavigationRail(
    topIcon: ImageVector,
    onTopIconClick: () -> Unit,
    tabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,

    showIcons: Boolean = true,

    loading: Boolean = false,

    topSpace: Dp = Dimensions.headerHeight,

    bottomContent: (@Composable () -> Unit)? = null,
    content: @Composable (@Composable (Int, String, ImageVector) -> Unit) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current

    val paddingValues = if (isLandscape) {
        Dimensions.navigationRailWidthLandscape
    } else {
        Dimensions.navigationRailWidth
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(paddingValues)
            .background(colorPalette.background0),
    ) {
        Box(
            Modifier.height(topSpace),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                Modifier
                    .offset(y = 12.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTopIconClick)
                    .padding(12.dp)
                    .size(22.dp),
            ) {
                Icon(
                    topIcon,
                    contentDescription = null,
                    tint = colorPalette.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.spacedBy(4.dp),

            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 8.dp),
        ) {
            content { index, label, icon ->
                val selected = index == tabIndex
                val fraction by animateFloatAsState(
                    if (selected) 1f else 0f,
                    label = "tab",
                )

                val iconContent: @Composable () -> Unit = if (!showIcons) {
                    {}
                } else {
                    {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = colorPalette.text,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = fraction
                                translationX = (1f - fraction) * -48.dp.toPx()
                                rotationZ = if (isLandscape) 0f else -90f
                            }
                            .size(Dimensions.navigationRailIconOffset * 2),
                    )
                    }
                }

                val textContent: @Composable () -> Unit = if (loading) {
                    {
                        Box(
                            Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .then(
                                    if (isLandscape) Modifier.width(72.dp).height(10.dp)
                                    else Modifier.width(10.dp).height(72.dp)
                                )
                                .clip(RoundedCornerShape(5.dp))
                                .shimmer()
                        )
                    }
                } else {
                    {
                    Text(
                        text = label,
                        style = typography.xs.semiBold,
                        color = if (selected) colorPalette.text else colorPalette.textDisabled,
                        modifier = Modifier
                            .vertical(enabled = !isLandscape)
                            .rotate(if (isLandscape) 0f else -90f)
                            .padding(horizontal = 16.dp),
                    )
                    }
                }

                val onClick = {
                    if (!selected && !loading) onTabIndexChange(index)
                }

                if (isLandscape) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onClick)
                            .padding(vertical = 8.dp),
                    ) {
                        iconContent()
                        textContent()
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onClick)
                            .padding(horizontal = 8.dp),
                    ) {
                        iconContent()
                        textContent()
                    }
                }
            }
        }

        bottomContent?.let {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) { it() }
        }
    }
}

private fun Modifier.vertical(enabled: Boolean) = if (!enabled) this else layout { measurable, constraints ->
    val placeable = measurable.measure(
        Constraints(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth,
        )
    )
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2),
        )
    }
}
