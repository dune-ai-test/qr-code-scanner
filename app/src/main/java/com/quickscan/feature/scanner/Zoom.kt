package com.quickscan.feature.scanner

/**
 * The detents a zoom step cycles through, and the clamping the controller
 * applies. Pulled out of the camera plumbing so the arithmetic can be tested
 * without a device: this is the part that decides whether a pinch past the
 * end of the lens stops or wraps.
 */
object Zoom {

    /** The stops a phone camera conventionally offers, in order. */
    val STEPS = listOf(0.5f, 1f, 2f, 3f, 5f, 10f)

    /** Detents closer than this are treated as the same stop. */
    const val EPSILON = 0.02f

    fun clamp(ratio: Float, min: Float, max: Float): Float {
        val ceiling = maxOf(max, min)
        return ratio.coerceIn(min, ceiling)
    }

    /** The stops this lens can actually reach, ascending. */
    fun availableSteps(min: Float, max: Float): List<Float> =
        STEPS.filter { it >= min - EPSILON && it <= max + EPSILON }

    /**
     * The next stop above [current], or the first one when already at the top.
     * Stops the lens cannot reach are skipped rather than silently clamped, so
     * tapping never appears to do nothing.
     */
    fun nextStep(current: Float, min: Float, max: Float): Float {
        val steps = availableSteps(min, max)
        if (steps.isEmpty()) return clamp(current, min, max)
        return steps.firstOrNull { it > current + EPSILON } ?: steps.first()
    }

    /** What the pill reads: "1x", "2x", "1.5x". */
    fun label(ratio: Float): String {
        val rounded = Math.round(ratio * 10f) / 10f
        val number = if (rounded % 1f == 0f) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
        return number + "x"
    }
}
