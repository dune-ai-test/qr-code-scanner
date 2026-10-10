package com.quickscan.data.barcode

/**
 * Decides when a camera frame is worth handing to the decoder.
 *
 * Analysing every frame of a 1280x720 stream is roughly 0.9 megapixels of
 * luminance work, thirty times a second, and almost all of it is wasted: the
 * scene is usually still, and a code that was readable a moment ago is still
 * readable now. Two cuts:
 *
 *  - **Rate limit.** Never more than [maxDecodesPerSecond] decodes a second.
 *    Detection stays responsive; the CPU does not.
 *  - **Change detection.** A cheap signature over a sparse grid of the frame.
 *    When the scene has not moved, the decode is skipped entirely.
 *
 * A still scene is still decoded at least once every [maxSilenceMillis], so a
 * slow change cannot be missed for long.
 *
 * The same signature drives [shouldCaptureFullRes]: when the viewfinder is
 * busy changing but the low-resolution stream has not read anything, it is
 * worth spending a full-resolution still on the scene.
 */
class FrameGate(
    private val maxDecodesPerSecond: Int = DEFAULT_MAX_DECODES_PER_SECOND,
    private val maxSilenceMillis: Long = DEFAULT_MAX_SILENCE_MILLIS,
    private val captureCooldownMillis: Long = DEFAULT_CAPTURE_COOLDOWN_MILLIS,
    private val changesBeforeCapture: Int = DEFAULT_CHANGES_BEFORE_CAPTURE,
) {
    private var lastDecodeAt = Long.MIN_VALUE
    private var lastCaptureAt = Long.MIN_VALUE
    private var lastSignature = 0L
    private var primed = false
    private var changesSinceCapture = 0

    /**
     * @param data the frame's luminance plane
     * @param offset index of the first sample
     * @param step sample stride; larger is cheaper and blunter
     */
    fun shouldDecode(data: ByteArray, offset: Int, step: Int, now: Long): Boolean {
        val signature = signature(data, offset, step)
        if (primed && signature != lastSignature) changesSinceCapture++

        val rateOk = lastDecodeAt == Long.MIN_VALUE || now - lastDecodeAt >= minIntervalMillis()
        val changed = !primed || signature != lastSignature
        val stale = now - lastDecodeAt >= maxSilenceMillis

        if (!rateOk) return false
        if (!changed && !stale) return false

        lastDecodeAt = now
        lastSignature = signature
        primed = true
        return true
    }

    /**
     * True when the viewfinder keeps changing but the live stream has not read
     * a code, so a full-resolution still is worth taking.
     *
     * Only when auto-detect is on; [CameraController] checks the flag and only
     * calls this to decide.
     */
    fun shouldCaptureFullRes(now: Long): Boolean {
        if (changesSinceCapture < changesBeforeCapture) return false
        if (lastCaptureAt != Long.MIN_VALUE && now - lastCaptureAt < captureCooldownMillis) {
            return false
        }
        changesSinceCapture = 0
        lastCaptureAt = now
        return true
    }

    /** Called after a successful decode so a found code does not also escalate. */
    fun onDecoded() {
        changesSinceCapture = 0
    }

    private fun minIntervalMillis(): Long = 1000L / maxDecodesPerSecond.coerceAtLeast(1)

    private fun signature(data: ByteArray, offset: Int, step: Int): Long {
        var hash = 1125899906842597L
        var i = offset
        val stride = step.coerceAtLeast(1)
        while (i < data.size) {
            hash = 31 * hash + data[i].toLong()
            i += stride
        }
        return hash
    }

    companion object {
        /** Fast enough to feel instant, slow enough to cut the work to a third. */
        const val DEFAULT_MAX_DECODES_PER_SECOND = 10

        /** Never go longer than this without decoding, even on a still scene. */
        const val DEFAULT_MAX_SILENCE_MILLIS = 750L

        /** At most one full-resolution still every this long. */
        const val DEFAULT_CAPTURE_COOLDOWN_MILLIS = 2_500L

        /** How many scene changes to see before spending a still on it. */
        const val DEFAULT_CHANGES_BEFORE_CAPTURE = 3
    }
}