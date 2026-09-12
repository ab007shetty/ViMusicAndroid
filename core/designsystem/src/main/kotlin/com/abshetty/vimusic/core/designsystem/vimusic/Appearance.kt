package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abshetty.vimusic.core.designsystem.R

@Immutable
data class ColorPalette(
    val background0: Color,
    val background1: Color,
    val background2: Color,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val red: Color = Color(0xffbf4040),
    val blue: Color = Color(0xff4472cf),
    val isDark: Boolean = true,
) {
    val favouritesIcon: Color get() = red
}

val DefaultDarkPalette = ColorPalette(
    background0 = Color(0xff16171d),
    background1 = Color(0xff1f2029),
    background2 = Color(0xff2b2d3b),
    accent = Color(0xff5055c0),
    onAccent = Color.White,
    text = Color(0xffe1e1e2),
    textSecondary = Color(0xffa3a4a6),
    textDisabled = Color(0xff6f6f73),
    isDark = true,
)

val DefaultLightPalette = ColorPalette(
    background0 = Color(0xfffdfdfe),
    background1 = Color(0xfff8f8fc),
    background2 = Color(0xffeaeaf5),
    accent = Color(0xff5055c0),
    onAccent = Color.White,
    text = Color(0xff212121),
    textSecondary = Color(0xff656566),
    textDisabled = Color(0xff9d9d9d),
    isDark = false,
)

val PureBlackPalette = DefaultDarkPalette.copy(
    background0 = Color.Black,
    background1 = Color.Black,
    background2 = Color.Black,
)

@Immutable
data class Typography(
    val xxs: TextStyle,
    val xs: TextStyle,
    val s: TextStyle,
    val m: TextStyle,
    val l: TextStyle,
    val xxl: TextStyle,
) {
    fun copyColor(color: Color) = Typography(
        xxs.copy(color = color), xs.copy(color = color), s.copy(color = color),
        m.copy(color = color), l.copy(color = color), xxl.copy(color = color),
    )
}

private val Poppins = FontFamily(
    Font(R.font.poppins_w300, FontWeight.Light),
    Font(R.font.poppins_w400, FontWeight.Normal),
    Font(R.font.poppins_w500, FontWeight.Medium),
    Font(R.font.poppins_w600, FontWeight.SemiBold),
    Font(R.font.poppins_w700, FontWeight.Bold),
)

fun typographyOf(color: Color): Typography {
    val base = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Normal,
        color = color,
    )
    return Typography(
        xxs = base.copy(fontSize = 12.sp),
        xs = base.copy(fontSize = 14.sp),
        s = base.copy(fontSize = 16.sp),
        m = base.copy(fontSize = 18.sp),
        l = base.copy(fontSize = 20.sp),
        xxl = base.copy(fontSize = 32.sp),
    )
}

val TextStyle.light: TextStyle get() = copy(fontWeight = FontWeight.Light)
val TextStyle.medium: TextStyle get() = copy(fontWeight = FontWeight.Medium)
val TextStyle.semiBold: TextStyle get() = copy(fontWeight = FontWeight.SemiBold)
val TextStyle.bold: TextStyle get() = copy(fontWeight = FontWeight.Bold)

@Immutable
data class Appearance(
    val colorPalette: ColorPalette,
    val typography: Typography,
    val thumbnailShape: Shape,
)

val LocalAppearance = staticCompositionLocalOf {
    Appearance(
        colorPalette = DefaultDarkPalette,
        typography = typographyOf(DefaultDarkPalette.text),
        thumbnailShape = RoundedCornerShape(ThumbnailRadius),
    )
}

val ThumbnailRadius: Dp = 8.dp

object Dimensions {
    val itemsVerticalPadding = 8.dp

    val navigationRailWidth = 50.dp
    val navigationRailWidthLandscape = 128.dp
    val navigationRailIconOffset = 6.dp
    val headerHeight = 140.dp
    val collapsedPlayer = 64.dp

    object thumbnails {
        val album = 128.dp
        val artist = 192.dp
        val song = 54.dp
        val playlist = album
    }
}
