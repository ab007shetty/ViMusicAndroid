package com.abshetty.vimusic.feature.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerGesturesTest {
    private val t = GestureThresholds(
        horizontalDistance = 120f, verticalDistance = 120f, velocity = 800f
    )

    @Test fun `a small drag settles back with no action`() {
        assertThat(GestureResolver.resolveSwipe(20f, 10f, 100f, t))
            .isEqualTo(GestureOutcome.Settle)
    }

    @Test fun `a long left drag skips to the next track`() {
        assertThat(GestureResolver.resolveSwipe(-200f, 5f, 300f, t))
            .isEqualTo(GestureOutcome.Next)
    }

    @Test fun `a long right drag goes to the previous track`() {
        assertThat(GestureResolver.resolveSwipe(200f, 5f, 300f, t))
            .isEqualTo(GestureOutcome.Previous)
    }

    @Test fun `a fast flick skips before reaching the distance threshold`() {
        assertThat(GestureResolver.resolveSwipe(-60f, 0f, 2000f, t))
            .isEqualTo(GestureOutcome.Next)
    }

    @Test fun `a downward drag collapses the player`() {
        assertThat(GestureResolver.resolveSwipe(5f, 200f, 300f, t))
            .isEqualTo(GestureOutcome.Collapse)
    }

    @Test fun `an upward drag expands the player`() {
        assertThat(GestureResolver.resolveSwipe(5f, -200f, 300f, t))
            .isEqualTo(GestureOutcome.Expand)
    }

    @Test fun `a diagonal drag commits to the dominant axis only`() {
        assertThat(GestureResolver.resolveSwipe(-200f, 150f, 300f, t))
            .isEqualTo(GestureOutcome.Next)
        assertThat(GestureResolver.resolveSwipe(-150f, 200f, 300f, t))
            .isEqualTo(GestureOutcome.Collapse)
    }

    @Test fun `exactly at the distance threshold commits`() {
        assertThat(GestureResolver.resolveSwipe(-120f, 0f, 0f, t))
            .isEqualTo(GestureOutcome.Next)
    }

    @Test fun `double-tapping the left third seeks backward`() {
        assertThat(GestureResolver.resolveDoubleTap(50f, 1080f))
            .isEqualTo(SeekDirection.BACKWARD)
    }

    @Test fun `double-tapping the right third seeks forward`() {
        assertThat(GestureResolver.resolveDoubleTap(1000f, 1080f))
            .isEqualTo(SeekDirection.FORWARD)
    }

    @Test fun `double-tapping the middle does nothing`() {
        assertThat(GestureResolver.resolveDoubleTap(540f, 1080f))
            .isEqualTo(SeekDirection.NONE)
    }
}
