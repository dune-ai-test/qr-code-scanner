package com.quickscan.data.barcode

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameGateTest {

    private fun frame(fill: Int, size: Int = 4096) = ByteArray(size) { fill.toByte() }

    @Test
    fun `the first frame always decodes`() {
        val gate = FrameGate()
        assertTrue(gate.shouldDecode(frame(1), 0, 1, now = 0))
    }

    @Test
    fun `an unchanged frame inside the rate limit is skipped`() {
        val gate = FrameGate(maxDecodesPerSecond = 10)
        val data = frame(7)

        assertTrue(gate.shouldDecode(data, 0, 1, now = 0))
        // 50ms later: inside the 100ms rate limit and the scene has not moved.
        assertFalse(gate.shouldDecode(data, 0, 1, now = 50))
    }

    @Test
    fun `a changed frame decodes once the rate limit allows`() {
        val gate = FrameGate(maxDecodesPerSecond = 10)

        assertTrue(gate.shouldDecode(frame(1), 0, 1, now = 0))
        assertTrue(gate.shouldDecode(frame(2), 0, 1, now = 150))
    }

    @Test
    fun `a still scene is still decoded after the silence limit`() {
        val gate = FrameGate(maxDecodesPerSecond = 10, maxSilenceMillis = 750)
        val data = frame(3)

        assertTrue(gate.shouldDecode(data, 0, 1, now = 0))
        // Past the rate limit but not yet past the silence limit.
        assertFalse(gate.shouldDecode(data, 0, 1, now = 200))
        // Past both: decode anyway so a slow change is not missed.
        assertTrue(gate.shouldDecode(data, 0, 1, now = 800))
    }

    @Test
    fun `decoding is capped at the configured rate`() {
        val gate = FrameGate(maxDecodesPerSecond = 10)
        var allowed = 0
        // One second at 30fps, a different frame every time so change
        // detection is never the thing that limits it.
        for (i in 0 until 30) {
            if (gate.shouldDecode(frame(i), 0, 1, now = i * 33L)) allowed++
        }
        assertTrue("allowed $allowed decodes in a second", allowed in 9..11)
    }

    @Test
    fun `a full resolution capture is not spent on the first change`() {
        val gate = FrameGate(changesBeforeCapture = 3)
        gate.shouldDecode(frame(1), 0, 1, now = 0)

        assertFalse(gate.shouldCaptureFullRes(now = 1_000))
    }

    @Test
    fun `a full resolution capture is spent after enough changes`() {
        val gate = FrameGate(changesBeforeCapture = 3, maxDecodesPerSecond = 10)
        for (i in 0..3) {
            gate.shouldDecode(frame(i + 1), 0, 1, now = i * 200L)
        }
        assertTrue(gate.shouldCaptureFullRes(now = 2_000))
    }

    @Test
    fun `captures respect a cooldown`() {
        val gate = FrameGate(changesBeforeCapture = 2, maxDecodesPerSecond = 10)
        for (i in 0..2) {
            gate.shouldDecode(frame(i + 1), 0, 1, now = i * 200L)
        }
        assertTrue(gate.shouldCaptureFullRes(now = 5_000))
        // Immediately after, the change budget has been spent again.
        for (i in 3..6) {
            gate.shouldDecode(frame(i + 1), 0, 1, now = 5_000 + i * 200L)
        }
        assertFalse(gate.shouldCaptureFullRes(now = 5_800))
    }

    @Test
    fun `a decoded code cancels a pending capture`() {
        val gate = FrameGate(changesBeforeCapture = 2)
        gate.shouldDecode(frame(1), 0, 1, now = 0)
        gate.shouldDecode(frame(2), 0, 1, now = 200)
        gate.onDecoded()

        assertFalse(gate.shouldCaptureFullRes(now = 1_000))
    }

    @Test
    fun `a larger sample step still separates different frames`() {
        val gate = FrameGate(maxDecodesPerSecond = 10)
        val a = frame(1)
        val b = ByteArray(4096) { (it * 7 % 251).toByte() }

        assertTrue(gate.shouldDecode(a, 0, 8, now = 0))
        assertTrue(gate.shouldDecode(b, 0, 8, now = 200))
    }
}