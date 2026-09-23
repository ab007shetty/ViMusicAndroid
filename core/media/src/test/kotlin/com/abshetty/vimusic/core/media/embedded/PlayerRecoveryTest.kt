package com.abshetty.vimusic.core.media.embedded

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRecoveryTest {

    @Test
    fun `a first failure reloads rather than giving up`() {
        assertEquals(Recovery.Reload(1), recoveryFor(0))
    }

    @Test
    fun `attempts count up to the cap`() {
        assertEquals(Recovery.Reload(1), recoveryFor(0))
        assertEquals(Recovery.Reload(2), recoveryFor(1))
        assertEquals(Recovery.Reload(3), recoveryFor(2))
    }

    @Test
    fun `past the cap it stops reloading`() {
        val outcome = recoveryFor(MAX_RELOAD_ATTEMPTS)

        assertTrue(outcome is Recovery.GiveUp)
    }

    @Test
    fun `giving up says something a person can act on`() {
        val outcome = recoveryFor(MAX_RELOAD_ATTEMPTS) as Recovery.GiveUp

        assertTrue(outcome.message.contains("connection"))
        assertTrue(outcome.message.isNotBlank())
    }

    @Test
    fun `it never loops forever`() {
        var attempts = 0
        var guard = 0
        while (recoveryFor(attempts) is Recovery.Reload && guard++ < 100) {
            attempts++
        }

        assertEquals(MAX_RELOAD_ATTEMPTS, attempts)
    }
}
