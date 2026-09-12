package com.abshetty.vimusic.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object BrandIcons {
    val Instagram: ImageVector by lazy {
        ImageVector.Builder(
            name = "Instagram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.5f, 2.75f)
                lineTo(16.5f, 2.75f)
                arcTo(4.75f, 4.75f, 0f, false, true, 21.25f, 7.5f)
                lineTo(21.25f, 16.5f)
                arcTo(4.75f, 4.75f, 0f, false, true, 16.5f, 21.25f)
                lineTo(7.5f, 21.25f)
                arcTo(4.75f, 4.75f, 0f, false, true, 2.75f, 16.5f)
                lineTo(2.75f, 7.5f)
                arcTo(4.75f, 4.75f, 0f, false, true, 7.5f, 2.75f)
                close()
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16.25f, 12f)
                arcTo(4.25f, 4.25f, 0f, true, true, 7.75f, 12f)
                arcTo(4.25f, 4.25f, 0f, true, true, 16.25f, 12f)
                close()
            }

            path(fill = SolidColor(Color.Black)) {
                moveTo(18.35f, 6.35f)
                arcTo(1.1f, 1.1f, 0f, true, true, 16.15f, 6.35f)
                arcTo(1.1f, 1.1f, 0f, true, true, 18.35f, 6.35f)
                close()
            }
        }.build()
    }
}
