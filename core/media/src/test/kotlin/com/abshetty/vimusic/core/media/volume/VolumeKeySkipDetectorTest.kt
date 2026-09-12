package com.abshetty.vimusic.core.media.volume

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VolumeKeySkipDetectorTest {
    private val threshold = 450L

    private fun detector() = VolumeKeySkipDetector(holdThresholdMs = threshold)

    @Test
    fun `a short press becomes one volume step on release`() {
        val detector = detector()

        val down = detector.onKey(
            SkipDirection.UP, downTime = 1_000, eventTime = 1_000, isRelease = false,
        )
        val up = detector.onKey(
            SkipDirection.UP, downTime = 1_000, eventTime = 1_100, isRelease = true,
        )

        assertThat(down).isEqualTo(VolumeKeyAction.Swallow)
        assertThat(up).isEqualTo(VolumeKeyAction.TapVolume(SkipDirection.UP))
    }

    @Test
    fun `a hold never asks for a volume step`() {
        val detector = detector()

        val events = listOf(1_000L, 1_100L, 1_200L, 1_600L, 2_000L).map { at ->
            detector.onKey(SkipDirection.UP, downTime = 1_000, eventTime = at, isRelease = false)
        }

        assertThat(events.filterIsInstance<VolumeKeyAction.TapVolume>()).isEmpty()
        assertThat(events.filterIsInstance<VolumeKeyAction.Skip>()).hasSize(1)
    }

    @Test
    fun `a repeat past the threshold skips`() {
        val detector = detector()

        detector.onKey(SkipDirection.UP, downTime = 1_000, eventTime = 1_000, isRelease = false)
        val held = detector.onKey(
            SkipDirection.UP, downTime = 1_000, eventTime = 1_000 + threshold, isRelease = false,
        )

        assertThat(held).isEqualTo(VolumeKeyAction.Skip(SkipDirection.UP))
    }

    @Test
    fun `a hold with no repeat events skips on release`() {
        val detector = detector()

        detector.onKey(SkipDirection.DOWN, downTime = 2_000, eventTime = 2_000, isRelease = false)
        val release = detector.onKey(
            SkipDirection.DOWN, downTime = 2_000, eventTime = 2_900, isRelease = true,
        )

        assertThat(release).isEqualTo(VolumeKeyAction.Skip(SkipDirection.DOWN))
    }

    @Test
    fun `the rest of a held press is swallowed rather than skipping again`() {
        val detector = detector()

        detector.onKey(SkipDirection.UP, downTime = 1_000, eventTime = 1_500, isRelease = false)
        val later = detector.onKey(
            SkipDirection.UP, downTime = 1_000, eventTime = 2_500, isRelease = false,
        )
        val release = detector.onKey(
            SkipDirection.UP, downTime = 1_000, eventTime = 3_000, isRelease = true,
        )

        assertThat(later).isEqualTo(VolumeKeyAction.Swallow)
        assertThat(release).isEqualTo(VolumeKeyAction.Swallow)
    }

    @Test
    fun `a fresh press after a skip can skip again`() {
        val detector = detector()

        detector.onKey(SkipDirection.UP, downTime = 1_000, eventTime = 1_500, isRelease = false)
        detector.reset()

        val second = detector.onKey(
            SkipDirection.UP, downTime = 4_000, eventTime = 4_500, isRelease = false,
        )

        assertThat(second).isEqualTo(VolumeKeyAction.Skip(SkipDirection.UP))
    }
}
