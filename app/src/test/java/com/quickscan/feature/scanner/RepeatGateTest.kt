package com.quickscan.feature.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The repeat rule behind continuous mode. Getting this wrong is loud — either
 * a code held in frame toasts every 750ms, or a genuine second approach goes
 * silent and the user has to walk away and back to re-read it.
 */
class RepeatGateTest {

    private var clock = 0L
    private fun gate(window: Long = 2_000L) = RepeatGate(window) { clock }

    @Test
    fun `a code seen for the first time is accepted`() {
        assertTrue(gate().shouldAccept("https://example.com"))
    }

    @Test
    fun `the same code again immediately is refused`() {
        val gate = gate()
        assertTrue(gate.shouldAccept("https://example.com"))
        assertFalse(gate.shouldAccept("https://example.com"))
    }

    @Test
    fun `a different code is not blocked by the first one`() {
        val gate = gate()
        assertTrue(gate.shouldAccept("code-a"))
        assertTrue(gate.shouldAccept("code-b"))
    }

    @Test
    fun `several distinct codes back to back all announce`() {
        val gate = gate()
        // The table case: a row of distinct codes, none of which may suppress
        // the next.
        repeat(20) { index ->
            assertTrue(gate.shouldAccept("code-$index"))
        }
    }

    @Test
    fun `holding one code still announces it exactly once`() {
        val gate = gate()
        assertTrue(gate.shouldAccept("menu"))
        // Frames keep arriving for as long as the code is held in view.
        repeat(40) {
            assertFalse(gate.shouldAccept("menu"))
        }
    }

    @Test
    fun `returning to a code after another one announces it again`() {
        val gate = gate()
        assertTrue(gate.shouldAccept("menu"))
        assertTrue(gate.shouldAccept("wifi"))
        // Back to the menu immediately. The previous payload was a different
        // one, so this is a genuine second approach, not a repeat.
        assertTrue(gate.shouldAccept("menu"))
    }

    @Test
    fun `the same code is accepted again once the window passes`() {
        val gate = gate(window = 2_000L)
        assertTrue(gate.shouldAccept("code"))
        clock = 1_999L
        assertFalse(gate.shouldAccept("code"))
        clock = 2_000L
        assertTrue(gate.shouldAccept("code"))
    }

    @Test
    fun `alternating two codes announces each approach`() {
        val gate = gate()
        repeat(5) {
            assertTrue(gate.shouldAccept("a"))
            assertTrue(gate.shouldAccept("b"))
        }
    }

    @Test
    fun `a long pause makes the same code eligible again`() {
        val gate = gate(window = 2_000L)
        assertTrue(gate.shouldAccept("code"))
        clock = 60_000L
        assertTrue(gate.shouldAccept("code"))
        // ...and the window restarts from there, so the next frame is refused.
        clock = 60_500L
        assertFalse(gate.shouldAccept("code"))
    }
}