package com.quickscan.core.qr

/**
 * Deterministic QR-shaped artwork for places that show a code without carrying
 * real content — the onboarding hero and the scanner's sample poster. The
 * output is a genuine [size]x[size] module grid: three finder patterns with
 * separators, timing rows, an alignment block, the dark module and reserved
 * format-information areas, with the remaining data area filled from a seeded
 * generator so the same seed always paints the same code.
 */
class QrPlaceholderGrid(val size: Int, private val modules: BooleanArray) {

    operator fun get(x: Int, y: Int): Boolean =
        x in 0 until size && y in 0 until size && modules[y * size + x]

    /** Row [y] as run-length segments, so a renderer emits one rect per run. */
    fun runsInRow(y: Int): List<IntRange> {
        val runs = mutableListOf<IntRange>()
        var start = -1
        for (x in 0 until size) {
            val dark = this[x, y]
            if (dark && start < 0) start = x
            if (!dark && start >= 0) {
                runs += start until x
                start = -1
            }
        }
        if (start >= 0) runs += start until size
        return runs
    }
}

object QrPlaceholder {

    /** Version 2 — the size the mockups use. */
    const val DEFAULT_SIZE = 25

    private const val MIN_SIZE = 21
    private const val MAX_SIZE = 57

    /**
     * @param seed any long; equal seeds always yield an equal grid.
     * @param size module count per edge, from 21 to 57 (version 1 to 10).
     */
    fun generate(seed: Long, size: Int = DEFAULT_SIZE): QrPlaceholderGrid {
        require(size in MIN_SIZE..MAX_SIZE) { "size must be $MIN_SIZE..$MAX_SIZE" }

        val modules = BooleanArray(size * size)
        val reserved = BooleanArray(size * size)

        fun mark(x: Int, y: Int, dark: Boolean, keep: Boolean = false) {
            if (x !in 0 until size || y !in 0 until size) return
            modules[y * size + x] = dark
            if (keep) reserved[y * size + x] = true
        }

        // Finder pattern plus its one-module light separator.
        fun drawFinder(ox: Int, oy: Int) {
            for (y in -1..7) {
                for (x in -1..7) {
                    val ring = x in 0..6 && y in 0..6
                    val dark = when {
                        !ring -> false                                // separator
                        x == 0 || x == 6 || y == 0 || y == 6 -> true   // outer ring
                        x in 2..4 && y in 2..4 -> true                 // 3x3 core
                        else -> false
                    }
                    mark(ox + x, oy + y, dark, keep = true)
                }
            }
        }

        fun drawAlignment(ox: Int, oy: Int) {
            for (y in 0..4) {
                for (x in 0..4) {
                    val onRing = x == 0 || x == 4 || y == 0 || y == 4
                    mark(ox + x, oy + y, onRing || (x == 2 && y == 2), keep = true)
                }
            }
        }

        // The three finder patterns.
        drawFinder(0, 0)
        drawFinder(size - 7, 0)
        drawFinder(0, size - 7)

        // Timing patterns run along row 6 and column 6.
        for (i in 0 until size) {
            val dark = i % 2 == 0
            mark(6, i, dark, keep = true)
            mark(i, 6, dark, keep = true)
        }

        // Alignment patterns, skipping any that would collide with a finder.
        val version = (size - 17) / 4
        val centers = alignmentCenters(version, size)
        for (cy in centers) {
            for (cx in centers) {
                if (isFinderRegion(cx, cy, size)) continue
                drawAlignment(cx - 2, cy - 2)
            }
        }

        // Format information area (decoded by real readers, painted blank here)
        // plus the always-dark module.
        for (i in 0..8) {
            if (i != 6) {
                mark(i, 8, dark = false, keep = true)
                mark(8, i, dark = false, keep = true)
            }
        }
        for (i in 0 until 8) {
            mark(size - 1 - i, 8, dark = false, keep = true)
            mark(8, size - 1 - i, dark = false, keep = true)
        }
        mark(8, 4 * version + 9, dark = true, keep = true)

        // Version information blocks exist from version 7 upward.
        if (version >= 7) {
            for (i in 0 until 18) {
                val a = i / 3
                val b = i % 3
                mark(size - 11 + b, a, dark = false, keep = true)
                mark(a, size - 11 + b, dark = false, keep = true)
            }
        }

        // Data area.
        val random = Lcg(seed)
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (reserved[y * size + x]) continue
                mark(x, y, dark = random.nextBit(), keep = false)
            }
        }

        return QrPlaceholderGrid(size, modules)
    }


    /** Spec 6.3.1.1: count, step, then walk up from the bottom-right centre. */
    private fun alignmentCenters(version: Int, size: Int): List<Int> {
        if (version < 2) return emptyList()
        val count = version / 7 + 2
        val step = (version * 4 + count * 2 + 1) / (count * 2 - 2) * 2
        val centers = mutableListOf<Int>()
        var pos = size - 7
        repeat(count - 1) {
            centers += pos
            pos -= step
        }
        centers += 6
        return centers.sorted()
    }

    /** True when an alignment pattern centred here would sit under a finder. */
    private fun isFinderRegion(cx: Int, cy: Int, size: Int): Boolean =
        (cx <= 8 && cy <= 8) ||
            (cx >= size - 9 && cy <= 8) ||
            (cx <= 8 && cy >= size - 9)
}

/** Numerical Recipes linear congruential generator; stable across platforms. */
internal class Lcg(seed: Long) {
    private var state: Long = (seed * 6364136223846793005L + 1442695040888963407L) and Long.MAX_VALUE

    private fun next(): Long {
        state = (state * 6364136223846793005L + 1442695040888963407L) and Long.MAX_VALUE
        return state shr 33
    }

    fun nextBit(): Boolean = next() and 1L == 1L
}