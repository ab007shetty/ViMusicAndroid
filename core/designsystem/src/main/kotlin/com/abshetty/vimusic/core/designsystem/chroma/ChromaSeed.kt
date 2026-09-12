package com.abshetty.vimusic.core.designsystem.chroma

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

object ChromaSeed {
    private const val BITMAP_AREA = 112 * 112
    private const val MAX_COLORS = 16

    fun fromBitmap(bitmap: Bitmap): Color {
        val palette = Palette.from(bitmap)

            .resizeBitmapArea(BITMAP_AREA)
            .maximumColorCount(MAX_COLORS)
            .generate()

        val rgb = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: return ChromaScheme.DEFAULT_SEED

        return Color(rgb)
    }
}
