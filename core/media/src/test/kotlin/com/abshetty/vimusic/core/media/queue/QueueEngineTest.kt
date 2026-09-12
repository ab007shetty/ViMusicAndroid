package com.abshetty.vimusic.core.media.queue

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class QueueEngineTest {
    private val engine = QueueEngine(random = Random(seed = 42))

    private fun state(
        size: Int = 3,
        index: Int = 0,
        shuffle: Boolean = false,
        repeat: RepeatMode = RepeatMode.OFF,
    ) = QueueState(
        items = List(size) { "song" + it }, index = index, shuffle = shuffle, repeat = repeat
    )

    @Test fun `skip next advances by one`() {
        assertThat(engine.onSkipNext(state(index = 0))).isEqualTo(QueueTransition.MoveTo(1))
    }

    @Test fun `skip next at the end with repeat off stops`() {
        assertThat(engine.onSkipNext(state(index = 2, repeat = RepeatMode.OFF)))
            .isEqualTo(QueueTransition.Stop)
    }

    @Test fun `skip next at the end with repeat all wraps to the start`() {
        assertThat(engine.onSkipNext(state(index = 2, repeat = RepeatMode.ALL)))
            .isEqualTo(QueueTransition.MoveTo(0))
    }

    @Test fun `skip next ignores repeat one`() {
        assertThat(engine.onSkipNext(state(index = 0, repeat = RepeatMode.ONE)))
            .isEqualTo(QueueTransition.MoveTo(1))
    }

    @Test fun `skip next with shuffle stays inside the queue`() {
        val transition = engine.onSkipNext(state(size = 5, shuffle = true))
        assertThat(transition).isInstanceOf(QueueTransition.MoveTo::class.java)
        assertThat((transition as QueueTransition.MoveTo).index).isIn(0..4)
    }

    @Test fun `skip next on an empty queue stops`() {
        assertThat(engine.onSkipNext(state(size = 0))).isEqualTo(QueueTransition.Stop)
    }

    @Test fun `track ending with repeat one restarts the same track`() {
        assertThat(engine.onTrackEnded(state(index = 1, repeat = RepeatMode.ONE)))
            .isEqualTo(QueueTransition.RestartCurrent)
    }

    @Test fun `track ending mid-queue advances`() {
        assertThat(engine.onTrackEnded(state(index = 0))).isEqualTo(QueueTransition.MoveTo(1))
    }

    @Test fun `track ending at the end with repeat off stops`() {
        assertThat(engine.onTrackEnded(state(index = 2, repeat = RepeatMode.OFF)))
            .isEqualTo(QueueTransition.Stop)
    }

    @Test fun `track ending at the end with repeat all wraps`() {
        assertThat(engine.onTrackEnded(state(index = 2, repeat = RepeatMode.ALL)))
            .isEqualTo(QueueTransition.MoveTo(0))
    }

    @Test fun `repeat one on an empty queue stops rather than restarting nothing`() {
        assertThat(engine.onTrackEnded(state(size = 0, repeat = RepeatMode.ONE)))
            .isEqualTo(QueueTransition.Stop)
    }

    @Test fun `skip previous goes back one`() {
        assertThat(engine.onSkipPrevious(state(index = 2))).isEqualTo(QueueTransition.MoveTo(1))
    }

    @Test fun `skip previous from the first track wraps to the last`() {
        assertThat(engine.onSkipPrevious(state(index = 0))).isEqualTo(QueueTransition.MoveTo(2))
    }

    @Test fun `skip previous on an empty queue stops`() {
        assertThat(engine.onSkipPrevious(state(size = 0))).isEqualTo(QueueTransition.Stop)
    }

    @Test fun `a one-track queue with repeat off stops at the end`() {
        assertThat(engine.onTrackEnded(state(size = 1, index = 0)))
            .isEqualTo(QueueTransition.Stop)
    }

    @Test fun `a one-track queue with repeat all replays it`() {
        assertThat(engine.onTrackEnded(state(size = 1, index = 0, repeat = RepeatMode.ALL)))
            .isEqualTo(QueueTransition.MoveTo(0))
    }

    @Test fun `shuffle is deterministic for a fixed seed`() {
        val a = QueueEngine(Random(7)).onSkipNext(state(size = 10, shuffle = true))
        val b = QueueEngine(Random(7)).onSkipNext(state(size = 10, shuffle = true))
        assertThat(a).isEqualTo(b)
    }
}
