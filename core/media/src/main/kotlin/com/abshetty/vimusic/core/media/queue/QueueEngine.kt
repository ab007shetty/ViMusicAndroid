package com.abshetty.vimusic.core.media.queue

import kotlin.random.Random

enum class RepeatMode { OFF, ALL, ONE }

data class QueueState(
    val items: List<String> = emptyList(),
    val index: Int = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
)

sealed interface QueueTransition {
    data class MoveTo(val index: Int) : QueueTransition
    data object RestartCurrent : QueueTransition
    data object Stop : QueueTransition
}

class QueueEngine constructor(
    private val random: Random = Random.Default,
) {
    fun onSkipNext(state: QueueState): QueueTransition = advance(state)

    fun onTrackEnded(state: QueueState): QueueTransition =
        if (state.repeat == RepeatMode.ONE && state.items.isNotEmpty()) {
            QueueTransition.RestartCurrent
        } else {
            advance(state)
        }

    fun onSkipPrevious(state: QueueState): QueueTransition {
        if (state.items.isEmpty()) return QueueTransition.Stop
        val previous = if (state.index <= 0) state.items.lastIndex else state.index - 1
        return QueueTransition.MoveTo(previous)
    }

    private fun advance(state: QueueState): QueueTransition {
        if (state.items.isEmpty()) return QueueTransition.Stop

        if (state.shuffle) return QueueTransition.MoveTo(random.nextInt(state.items.size))

        val atEnd = state.index >= state.items.lastIndex

        if (atEnd && state.repeat == RepeatMode.OFF) return QueueTransition.Stop

        return QueueTransition.MoveTo(if (atEnd) 0 else state.index + 1)
    }
}
