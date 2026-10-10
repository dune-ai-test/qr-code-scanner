package com.quickscan.feature.scanner

/**
 * Decides whether a decoded payload is a new scan or the same code still
 * sitting in the viewfinder.
 *
 * This only matters in continuous mode. Navigating mode leaves the scanner the
 * moment something is read, so the same frame is never decoded twice. Staying
 * put means a code held in front of the lens is decoded again every time the
 * frame gate lets a still scene through — roughly every 750ms — and every one
 * of those would otherwise announce itself.
 *
 * The rule is "once per approach": a payload is refused while it is the same
 * one just read, and is eligible again the moment something *different* is
 * read. So holding a code still announces it once, and returning to it after
 * dealing with the next one announces it again.
 *
 * Only the last payload is kept. That is all the rule needs, it cannot grow
 * during a long session, and it does not accumulate a record of what everyone
 * at a table happened to scan.
 */
class RepeatGate(
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastRaw: String? = null
    private var lastAt = 0L

    /**
     * True when [raw] should be treated as a fresh scan. A false result also
     * records nothing, so the caller can drop the frame outright.
     */
    fun shouldAccept(raw: String): Boolean {
        val at = now()
        if (raw == lastRaw && at - lastAt < windowMillis) return false
        lastRaw = raw
        lastAt = at
        return true
    }

    companion object {
        /**
         * A floor, not a delay. It only has to cover the gap between two
         * consecutive decodes of a still scene, which the frame gate puts at
         * about 750ms. Sitting comfortably above that leaves room for a
         * dropped frame without making a real second approach go quiet.
         */
        const val DEFAULT_WINDOW_MILLIS = 2_000L
    }
}