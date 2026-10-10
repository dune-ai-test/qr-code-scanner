package com.quickscan.feature.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomTest {

    @Test
    fun `clamping stops at the end of the lens`() {
        assertEquals(4f, Zoom.clamp(9f, min = 1f, max = 4f), 0.001f)
        assertEquals(1f, Zoom.clamp(0.2f, min = 1f, max = 4f), 0.001f)
        assertEquals(2.5f, Zoom.clamp(2.5f, min = 1f, max = 4f), 0.001f)
    }

    @Test
    fun `a fixed lens clamps to its only ratio`() {
        val only = Zoom.clamp(3f, min = 1f, max = 1f)
        assertEquals(1f, only, 0.001f)
    }

    @Test
    fun `only the stops the lens reaches are offered`() {
        // A lens that tops out at 2x does not advertise 5x or 10x.
        assertEquals(listOf(1f, 2f), Zoom.availableSteps(min = 1f, max = 2f))
        // One that reaches 10x gets the whole ladder.
        assertEquals(listOf(0.5f, 1f, 2f, 3f, 5f, 10f), Zoom.availableSteps(0.5f, 10f))
        // An ultrawide front lens that only zooms in.
        assertEquals(listOf(1f, 2f), Zoom.availableSteps(min = 1f, max = 2.5f))
    }

    @Test
    fun `stepping walks up the ladder and wraps at the top`() {
        var current = 1f
        current = Zoom.nextStep(current, 1f, 10f)
        assertEquals(2f, current, 0.001f)
        current = Zoom.nextStep(current, 1f, 10f)
        assertEquals(3f, current, 0.001f)
        current = Zoom.nextStep(current, 1f, 10f)
        assertEquals(5f, current, 0.001f)
        current = Zoom.nextStep(current, 1f, 10f)
        assertEquals(10f, current, 0.001f)
        // At the top it returns to the first stop the lens has rather than
        // sticking. A lens that cannot go below 1x wraps to 1x, not to 0.5x.
        current = Zoom.nextStep(current, 1f, 10f)
        assertEquals(1f, current, 0.001f)
    }

    @Test
    fun `stepping skips a stop the lens cannot reach`() {
        // From 1x on a 2x lens the next stop is 2x, not 3x.
        assertEquals(2f, Zoom.nextStep(1f, 1f, 2f), 0.001f)
        // From 2x on a 2x lens it wraps to the first, 1x.
        assertEquals(1f, Zoom.nextStep(2f, 1f, 2f), 0.001f)
    }

    @Test
    fun `a pinch resting slightly off a stop advances rather than sticking`() {
        // Without the epsilon a pinch that leaves zoom at 1.005 would look
        // like the step did nothing.
        assertEquals(2f, Zoom.nextStep(1.005f, 1f, 10f), 0.001f)
    }

    @Test
    fun `a lens with no range at all holds still`() {
        assertEquals(1f, Zoom.nextStep(1f, 1f, 1f), 0.001f)
        assertEquals(1f, Zoom.clamp(5f, 1f, 1f), 0.001f)
    }

    @Test
    fun `the readout trims a trailing zero but keeps a half`() {
        assertEquals("1x", Zoom.label(1f))
        assertEquals("2x", Zoom.label(2f))
        assertEquals("10x", Zoom.label(10f))
        assertEquals("0.5x", Zoom.label(0.5f))
        assertEquals("1.5x", Zoom.label(1.5f))
        assertEquals("2.3x", Zoom.label(2.25f))
    }

    @Test
    fun `availability follows the lens range`() {
        assertTrue(ZoomState(min = 1f, max = 4f, current = 1f).isAvailable)
        assertFalse(ZoomState(min = 1f, max = 1.02f, current = 1f).isAvailable)
        assertFalse(ZoomState(min = 1f, max = 1f, current = 1f).isAvailable)
    }
}
