package com.abshetty.vimusic.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ChromaTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(
            fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
        ),
        headlineMedium = headlineMedium.copy(
            fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp
        ),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium),

        bodySmall = TextStyle(
            fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Medium
        ),
    )
}
