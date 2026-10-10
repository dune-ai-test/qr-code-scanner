package com.quickscan.data.barcode

import kotlin.math.abs

/**
 * How close a frame is to reading, judged by what a QR code is made of.
 *
 * A failed decode says nothing about *why* it failed, which is what makes a
 * scan that is not working feel like a dead end: the viewfinder looks the
 * same whether there is nothing there or whether there is a code and it cannot
 * quite be read. Looking at the finder patterns separates those two, so the
 * viewfinder can say something useful instead of nothing.
 */
enum class ScanConfidence {
    /** Nothing in frame that resembles a code. */
    Nothing,

    /** Something code-shaped is arriving but will not read yet. */
    Partly,

    /** A whole QR code is framed; a decode is close behind. */
    Framed,
}

/**
 * Finds the finder patterns in a frame by looking for the 1:1:3:1:1 dark/light
 * run ratio they are built from.
 *
 * ZXing does this internally while trying to read a code, but only surfaces it
 * once a decode has already succeeded — the opposite of when it is useful.
 * Doing it here means the viewfinder knows what is in front of it while the
 * decode is still failing, which is the only time the answer is worth having.
 *
 * This is pure so it can be tested against synthetic rows. The run-length
 * state machine is ZXing's `FinderPatternFinder`, minus the cross-checking
 * and perspective transform: this only needs to answer "how many", not "where
 * and at what angle", which is a much easier question and costs about a tenth
 * of the work.
 */
object FinderPatternScan {

    /** Luma below this is a dark module. */
    private const val DARK = 128

    /** A pattern narrower than this is noise, not a module run. */
    private const val MIN_WIDTH = 21

    /** A QR code has three; fewer than that is a fragment. */
    private const val FULL_CODE_PATTERNS = 3

    /**
     * Centres closer than this are the same pattern seen on another scanline.
     * A tilted code drifts further between lines than a flat one, so this is
     * widened to the pattern's own width when one is known.
     */
    private const val CLUSTER_TOLERANCE = 12

    /** How many scanlines to sample. Enough to cross a code, few enough to be free. */
    const val LINE_COUNT = 14

    /**
     * @param rows sampled scanlines, each [width] luma bytes, indexed the same
     *   way the decoder's packed plane is: `row[x]`.
     */
    fun scan(rows: List<ByteArray>, width: Int): ScanConfidence {
        if (width < MIN_WIDTH || rows.isEmpty()) return ScanConfidence.Nothing

        val hits = ArrayList<Hit>(rows.size)
        for (row in rows) {
            hits += finderHits(row, width)
        }
        if (hits.isEmpty()) return ScanConfidence.Nothing

        return if (cluster(hits) >= FULL_CODE_PATTERNS) {
            ScanConfidence.Framed
        } else {
            ScanConfidence.Partly
        }
    }

    /** One finder-pattern candidate: where its centre is and how wide it is. */
    private class Hit(val centre: Int, val width: Int)

    /**
     * Every 1:1:3:1:1 run on one scanline.
     *
     * State alternates dark, light, dark, light, dark, so an odd state is
     * counting light pixels and an even one dark. Reaching state four with a
     * full set of runs is the only place a pattern can be declared.
     */
    private fun finderHits(row: ByteArray, width: Int): List<Hit> {
        val hits = mutableListOf<Hit>()
        val counts = IntArray(5)
        var state = 0
        var start = 0

        // Rows usually begin in light space. Consuming it as the pattern's outer
        // light run is survivable — the shift below undoes it — but resetting
        // here is what ZXing does and it is free.
        var x = 0
        while (x < width && row[x].luminance() >= DARK) x++

        while (x < width) {
            if (row[x].luminance() < DARK) {
                // Black: a light run just ended.
                if ((state and 1) == 1) state++
                counts[state]++
            } else if ((state and 1) == 0) {
                // White, and a dark run just ended.
                if (state == 4) {
                    if (isFinderRatio(counts)) {
                        hits += Hit(
                            centre = start + counts[0] + counts[1] + counts[2] / 2,
                            width = counts.sum(),
                        )
                        state = 0
                        counts.fill(0)
                    } else {
                        // A near miss is usually one module of drift at the
                        // edge of a pattern, so the runs shift left by one and
                        // scanning continues rather than abandoning the row.
                        start += counts[0] + counts[1]
                        state = 3
                        counts[0] = counts[2]
                        counts[1] = counts[3]
                        counts[2] = counts[4]
                        counts[3] = 1
                        counts[4] = 0
                    }
                } else {
                    // The first light run marks where the pattern's leading
                    // dark run began, which is what the centre is measured from.
                    if (state == 0) start = x - counts[0]
                    state++
                    counts[state]++
                }
            } else {
                counts[state]++
            }
            x++
        }
        return hits
    }

    /**
     * The five runs must be in the ratio 1:1:3:1:1, each within half a module.
     *
     * The centre run is held to the same half-module tolerance as the others
     * rather than ZXing's three, because ZXing pairs its looser centre with
     * cross-checks that verify the three patterns really do form a QR code.
     * There are no cross-checks here — only the count matters, not the
     * position — so the ratio has to carry the whole load, and at three times
     * the tolerance a 1:1:2:1:1 run passes as a finder pattern.
     */
    private fun isFinderRatio(counts: IntArray): Boolean {
        var total = 0
        for (count in counts) {
            if (count == 0) return false
            total += count
        }
        if (total < 7) return false
        val module = total / 7f
        val tolerance = module / 2f
        return abs(module - counts[0]) < tolerance &&
            abs(module - counts[1]) < tolerance &&
            abs(3f * module - counts[2]) < tolerance &&
            abs(module - counts[3]) < tolerance &&
            abs(module - counts[4]) < tolerance
    }

    /**
     * Groups hits that are the same pattern seen on different scanlines. A
     * pattern spanning `width` pixels can drift by about that much between
     * lines when the code is tilted, so the tolerance follows it.
     */
    private fun cluster(hits: List<Hit>): Int {
        if (hits.isEmpty()) return 0
        val sorted = hits.sortedBy { it.centre }
        var clusters = 1
        var previous = sorted.first()
        for (hit in sorted.drop(1)) {
            val tolerance = maxOf(CLUSTER_TOLERANCE, previous.width)
            if (hit.centre - previous.centre > tolerance) clusters++
            previous = hit
        }
        return clusters
    }

    private fun Byte.luminance(): Int = this.toInt() and 0xFF
}