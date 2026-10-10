package com.quickscan.core.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sheet geometry. This is arithmetic on how many codes fit on a printable
 * page, and the failure mode is arithmetic: a placement off the edge of the
 * bitmap throws at draw time on a device, not here.
 */
class QrContactSheetTest {

    @Test
    fun `no codes makes no sheet`() {
        val options = QrContactSheet.optionsFor(0)
        assertEquals(0, options.rows)
        assertTrue(options.width > 0)
        assertTrue(options.height > 0)
        assertTrue(QrContactSheet.placements(options).isEmpty())
    }

    @Test
    fun `one code is a single column`() {
        val options = QrContactSheet.optionsFor(1)
        assertEquals(1, options.columns)
        assertEquals(1, options.rows)
        assertTrue(options.isFullGrid)
    }

    @Test
    fun `two and four codes use two columns`() {
        assertEquals(2, QrContactSheet.optionsFor(2).columns)
        assertEquals(2, QrContactSheet.optionsFor(4).columns)
    }

    @Test
    fun `five to nine use three columns`() {
        assertEquals(3, QrContactSheet.optionsFor(5).columns)
        assertEquals(3, QrContactSheet.optionsFor(9).columns)
    }

    @Test
    fun `ten or more use the maximum`() {
        assertEquals(QrContactSheet.MAX_COLUMNS, QrContactSheet.optionsFor(10).columns)
    }

    @Test
    fun `columns never exceed the number of codes`() {
        // One code must not reserve a second empty column.
        assertEquals(1, QrContactSheet.optionsFor(1).columns)
        assertEquals(2, QrContactSheet.optionsFor(2).columns)
    }

    @Test
    fun `rows round up`() {
        assertEquals(1, QrContactSheet.optionsFor(2).rows)
        assertEquals(2, QrContactSheet.optionsFor(4).rows)
        assertEquals(2, QrContactSheet.optionsFor(5).rows)
        assertEquals(3, QrContactSheet.optionsFor(10).rows)
    }

    @Test
    fun `a short final row is not a full grid`() {
        assertTrue(!QrContactSheet.optionsFor(5).isFullGrid)
        assertTrue(QrContactSheet.optionsFor(6).isFullGrid)
    }

    @Test
    fun `the count is capped at the sheet limit`() {
        assertEquals(QrContactSheet.MAX_ITEMS, QrContactSheet.optionsFor(500).count)
    }

    @Test
    fun `there is one placement per code`() {
        for (count in listOf(1, 2, 5, 7, 12, 60)) {
            val options = QrContactSheet.optionsFor(count)
            assertEquals(count, QrContactSheet.placements(options).size)
        }
    }

    @Test
    fun `every placement sits inside the sheet`() {
        // The failure this guards: a placement past the bitmap edge throws from
        // Canvas.drawBitmap, on a device, with no test to have caught it.
        for (count in listOf(1, 2, 3, 5, 8, 9, 11, 17, 40, 60)) {
            val options = QrContactSheet.optionsFor(count)
            for (place in QrContactSheet.placements(options)) {
                assertTrue(
                    "code ${place.index} overflows ${options.width}x${options.height}",
                    place.codeLeft >= 0 && place.codeTop >= 0 &&
                        place.codeLeft + place.codeSize <= options.width &&
                        place.codeTop + options.cellHeight <= options.height,
                )
            }
        }
    }

    @Test
    fun `every label has room inside the sheet`() {
        // The cell is sized for a caption as well as a code. Without this a
        // sheet height that forgets the caption band clips every label on the
        // page and the geometry tests all still pass.
        for (count in listOf(1, 4, 9, 17, 40, 60)) {
            val options = QrContactSheet.optionsFor(count)
            for (place in QrContactSheet.placements(options)) {
                assertTrue(
                    "label ${place.index} overflows ${options.width}x${options.height}",
                    place.labelLeft + place.labelWidth <= options.width &&
                        place.labelTop < options.height,
                )
            }
        }
    }

    @Test
    fun `placements never overlap`() {
        val options = QrContactSheet.optionsFor(12)
        val places = QrContactSheet.placements(options)
        for (i in places.indices) {
            for (j in i + 1 until places.size) {
                val a = places[i]
                val b = places[j]
                val separated = a.codeLeft + a.codeSize <= b.codeLeft ||
                    b.codeLeft + b.codeSize <= a.codeLeft ||
                    a.codeTop + a.codeSize <= b.codeTop ||
                    b.codeTop + b.codeSize <= a.codeTop
                assertTrue("codes ${a.index} and ${b.index} overlap", separated)
            }
        }
    }

    @Test
    fun `the sheet grows with the number of codes`() {
        val one = QrContactSheet.optionsFor(1)
        val many = QrContactSheet.optionsFor(20)
        assertTrue(many.width > one.width)
        assertTrue(many.height > one.height)
    }

    @Test
    fun `a single code keeps a square cell`() {
        val options = QrContactSheet.optionsFor(1)
        assertEquals(options.codeSize, options.cellWidth)
    }
}