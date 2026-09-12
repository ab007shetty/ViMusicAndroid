package com.abshetty.vimusic.core.designsystem.chroma

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContrastGuardTest {
    @Test fun `black on white is the maximum ratio`() {
        assertThat(ContrastGuard.ratio(Color.Black, Color.White)).isWithin(0.01).of(21.0)
    }

    @Test fun `a colour against itself is the minimum ratio`() {
        assertThat(ContrastGuard.ratio(Color.Red, Color.Red)).isWithin(0.01).of(1.0)
    }

    @Test fun `the ratio is symmetric`() {
        val a = ContrastGuard.ratio(Color(0xFF3366CC), Color(0xFFF0F0F0))
        val b = ContrastGuard.ratio(Color(0xFFF0F0F0), Color(0xFF3366CC))
        assertThat(a).isWithin(0.001).of(b)
    }

    @Test fun `a readable pairing is returned unchanged`() {
        val fg = Color.Black
        assertThat(ContrastGuard.ensureReadable(fg, Color.White, minRatio = 4.5)).isEqualTo(fg)
    }

    @Test fun `an unreadable foreground is darkened until it passes`() {
        val result = ContrastGuard.ensureReadable(Color(0xFFEFEFEF), Color.White, minRatio = 4.5)

        assertThat(ContrastGuard.ratio(result, Color.White)).isAtLeast(4.5)
    }

    @Test fun `an unreadable foreground on a dark surface is lightened`() {
        val result = ContrastGuard.ensureReadable(Color(0xFF101010), Color.Black, minRatio = 4.5)

        assertThat(ContrastGuard.ratio(result, Color.Black)).isAtLeast(4.5)
    }

    @Test fun `clamping preserves the hue family`() {
        val result = ContrastGuard.ensureReadable(Color(0xFFBBD0FF), Color.White, minRatio = 4.5)

        assertThat(result.blue).isGreaterThan(result.red)
    }

    @Test fun `alpha is preserved through clamping`() {
        val translucent = Color(0xFFEFEFEF).copy(alpha = 0.5f)
        val result = ContrastGuard.ensureReadable(translucent, Color.White, minRatio = 4.5)

        assertThat(result.alpha).isWithin(0.0001f).of(translucent.alpha)
    }

    @Test fun `an impossible requirement terminates rather than looping`() {
        assertThat(ContrastGuard.ensureReadable(Color.Gray, Color(0xFF808080), minRatio = 21.0))
            .isNotNull()
    }

    @Test fun `meetsAA agrees with the ratio threshold`() {
        assertThat(ContrastGuard.meetsAA(Color.Black, Color.White)).isTrue()
        assertThat(ContrastGuard.meetsAA(Color(0xFFEFEFEF), Color.White)).isFalse()
    }

    @Test fun `luminance orders light above dark`() {
        assertThat(ContrastGuard.luminance(Color.White))
            .isGreaterThan(ContrastGuard.luminance(Color.Black))
    }
}
