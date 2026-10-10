package com.quickscan.data.barcode

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The confidence classification, against scanlines built to a known shape.
 *
 * The rows here are synthetic rather than photographed, which is the point: a
 * finder pattern is a fixed 1:1:3:1:1 run ratio, so a row containing one can be
 * written down exactly. What cannot be written down is how often this agrees
 * with a real code at a real angle, and that is the part no unit test reaches.
 */
class FinderPatternScanTest {

    @Test
    fun `an empty frame is nothing`() {
        assertEquals(ScanConfidence.Nothing, scan(listOf(blank(W)), W))
    }

    @Test
    fun `an all-dark frame is nothing`() {
        val rows = listOf(blank(W, BLACK))
        assertEquals(ScanConfidence.Nothing, scan(rows, W))
    }

    @Test
    fun `a frame too narrow to hold a pattern is nothing`() {
        assertEquals(ScanConfidence.Nothing, scan(listOf(blank(10)), 10))
    }

    @Test
    fun `no scanlines is nothing`() {
        assertEquals(ScanConfidence.Nothing, scan(emptyList(), W))
    }

    @Test
    fun `one finder pattern is only partial`() {
        val rows = listOf(finderRow(W, module = 4, at = 40))
        assertEquals(ScanConfidence.Partly, scan(rows, W))
    }

    @Test
    fun `two finder patterns are still only partial`() {
        val rows = listOf(withPatterns(W, 4, 40, 200))
        assertEquals(ScanConfidence.Partly, scan(rows, W))
    }

    @Test
    fun `three finder patterns read as a framed code`() {
        val rows = listOf(withPatterns(W, 4, 30, 140, 240))
        assertEquals(ScanConfidence.Framed, scan(rows, W))
    }

    @Test
    fun `a code tilted across scanlines still counts three`() {
        // The same three patterns drift further apart on each line, which is
        // what a code held at an angle looks like. Drift within a pattern's
        // own width has to cluster, or a tilted code reads as six.
        val rows = listOf(
            withPatterns(W, 4, 30, 140, 240),
            withPatterns(W, 4, 39, 149, 249),
            withPatterns(W, 4, 48, 158, 258),
        )
        assertEquals(ScanConfidence.Framed, scan(rows, W))
    }

    @Test
    fun `the state machine recovers from a run that is not a pattern`() {
        // A long dark run derails the ratios. Without the shift the valid
        // patterns after it would never be seen and this would report nothing.
        val row = blank(W).also {
            dark(it, 100, 20)
            finderInto(it, 4, 40)
            finderInto(it, 4, 200)
        }
        assertEquals(ScanConfidence.Partly, scan(listOf(row), W))
    }

    @Test
    fun `three patterns around a bad run are still framed`() {
        val row = blank(W).also {
            dark(it, 100, 20)
            finderInto(it, 4, 30)
            finderInto(it, 4, 160)
            finderInto(it, 4, 250)
        }
        assertEquals(ScanConfidence.Framed, scan(listOf(row), W))
    }

    @Test
    fun `the right shape with the wrong ratio is not a pattern`() {
        // 1:1:2:1:1. ZXing's own centre tolerance is three times the others
        // because it backs the ratio up with cross-checks; there are none here,
        // so the centre is held to the same half-module as the rest.
        val row = blank(W).also {
            dark(it, 40, 4)
            dark(it, 48, 8)
            dark(it, 60, 4)
        }
        assertEquals(ScanConfidence.Nothing, scan(listOf(row), W))
    }

    @Test
    fun `too much drift is not a pattern`() {
        val row = blank(W).also {
            dark(it, 40, 12)
            dark(it, 56, 20)
            dark(it, 80, 12)
        }
        assertEquals(ScanConfidence.Nothing, scan(listOf(row), W))
    }

    @Test
    fun `a module size that does not divide evenly still reads`() {
        val rows = listOf(finderRow(W, module = 3, at = 50))
        assertEquals(ScanConfidence.Partly, scan(rows, W))
    }

    @Test
    fun `a large module size reads`() {
        val rows = listOf(withPatterns(W, 8, 20, 110, 200))
        assertEquals(ScanConfidence.Framed, scan(rows, W))
    }

    @Test
    fun `a row that begins in light space still reads`() {
        val rows = listOf(finderRow(W, module = 4, at = 120))
        assertEquals(ScanConfidence.Partly, scan(rows, W))
    }

    private companion object {
        const val W = 300
        const val LIGHT = 230
        const val BLACK = 20

        fun scan(rows: List<ByteArray>, width: Int) =
            FinderPatternScan.scan(rows, width)

        fun blank(width: Int, value: Int = LIGHT) =
            ByteArray(width) { value.toByte() }

        fun dark(row: ByteArray, from: Int, length: Int) {
            for (i in from until from + length) row[i] = BLACK.toByte()
        }

        /** A centre line through one finder pattern: 1:1:3:1:1 dark to light. */
        fun finderRow(width: Int, module: Int, at: Int): ByteArray {
            val row = blank(width)
            val runs = listOf(BLACK, LIGHT, BLACK, LIGHT, BLACK)
            val lengths = listOf(module, module, 3 * module, module, module)
            var x = at
            runs.forEachIndexed { index, value ->
                repeat(lengths[index]) {
                    row[x++] = value.toByte()
                }
            }
            return row
        }

        /** Lays one pattern over an existing row, leaving the rest alone. */
        fun finderInto(row: ByteArray, module: Int, at: Int) {
            val pattern = finderRow(row.size, module, at)
            for (i in pattern.indices) {
                if (pattern[i] != LIGHT.toByte()) row[i] = pattern[i]
            }
        }

        fun withPatterns(width: Int, module: Int, vararg at: Int): ByteArray {
            val row = blank(width)
            at.forEach { finderInto(row, module, it) }
            return row
        }
    }
}