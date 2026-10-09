package com.quickscan.core.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPlaceholderTest {

    @Test
    fun `default grid is 25 modules across, matching the mockups`() {
        assertEquals(25, QrPlaceholder.DEFAULT_SIZE)
        assertEquals(25, QrPlaceholder.generate(seed = 1).size)
    }

    @Test
    fun `the same seed always paints the same grid`() {
        val first = QrPlaceholder.generate(seed = 42L)
        val second = QrPlaceholder.generate(seed = 42L)

        for (y in 0 until first.size) {
            for (x in 0 until first.size) {
                assertEquals("mismatch at $x,$y", first[x, y], second[x, y])
            }
        }
    }

    @Test
    fun `different seeds paint different data areas`() {
        val a = QrPlaceholder.generate(seed = 1L)
        val b = QrPlaceholder.generate(seed = 2L)

        val differences = (0 until a.size).sumOf { y ->
            (0 until a.size).count { x -> a[x, y] != b[x, y] }
        }
        assertTrue("expected the data area to differ, got $differences modules", differences > 10)
    }

    @Test
    fun `the top-left finder pattern is a correct 7 by 7`() {
        val grid = QrPlaceholder.generate(seed = 7L)

        for (i in 0..6) {
            assertTrue("ring at $i,0", grid[i, 0])
            assertTrue("ring at 0,$i", grid[0, i])
            assertTrue("ring at $i,6", grid[i, 6])
            assertTrue("ring at 6,$i", grid[6, i])
        }
        assertFalse("corner gap at 1,1", grid[1, 1])
        for (i in 2..4) {
            for (j in 2..4) {
                assertTrue("core at $i,$j", grid[i, j])
            }
        }
        assertFalse("gap at 1,2", grid[1, 2])
        assertFalse("gap at 2,1", grid[2, 1])
    }

    @Test
    fun `all three finder patterns are present`() {
        val grid = QrPlaceholder.generate(seed = 7L)
        val n = grid.size

        assertTrue("top-right", grid[n - 1, 0])
        assertTrue("bottom-left", grid[0, n - 1])
        assertTrue("bottom-right corner", grid[n - 1, n - 1])
    }

    @Test
    fun `separators around the finders are light`() {
        val grid = QrPlaceholder.generate(seed = 7L)
        val n = grid.size

        assertFalse("above top-left", grid[0, 7])
        assertFalse("left of top-left", grid[7, 0])
        assertFalse("below bottom-left", grid[0, n - 8])
        assertFalse("right of top-right", grid[n - 8, 0])
    }

    @Test
    fun `timing row and column alternate`() {
        val grid = QrPlaceholder.generate(seed = 7L)

        for (i in 8 until grid.size - 8) {
            assertEquals("row 6 at $i", i % 2 == 0, grid[i, 6])
            assertEquals("col 6 at $i", i % 2 == 0, grid[6, i])
        }
    }

    @Test
    fun `version 2 has one alignment block at 18,18`() {
        val grid = QrPlaceholder.generate(seed = 7L)

        assertTrue("alignment outer corner", grid[16, 16])
        assertTrue("alignment core", grid[18, 18])
        assertFalse("alignment gap", grid[17, 17])
        assertFalse("alignment ring gap", grid[17, 18])
    }

    @Test
    fun `the dark module is set for version 2`() {
        val grid = QrPlaceholder.generate(seed = 7L)
        assertTrue(grid[8, 17])
    }

    @Test
    fun `a different size produces the matching geometry`() {
        val grid = QrPlaceholder.generate(seed = 7L, size = 33)

        assertEquals(33, grid.size)
        assertTrue("top-left finder", grid[0, 0])
        assertTrue("top-right finder", grid[32, 0])
        assertTrue("bottom-left finder", grid[0, 32])
    }

    @Test
    fun `runs tile each row exactly once`() {
        val grid = QrPlaceholder.generate(seed = 3L)

        for (y in 0 until grid.size) {
            val covered = grid.runsInRow(y).sumOf { it.count() }
            val dark = (0 until grid.size).count { x -> grid[x, y] }
            assertEquals("row $y", dark, covered)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an unsupported size is rejected`() {
        QrPlaceholder.generate(seed = 1L, size = 13)
    }

    @Test
    fun `reading outside the grid is safe`() {
        val grid = QrPlaceholder.generate(seed = 1L)
        assertFalse(grid[-1, 0])
        assertFalse(grid[0, 999])
    }
}