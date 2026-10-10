package com.quickscan.data.barcode

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rotation is where the camera bug lived: an off-by-one in the row loop
 * left half the image black, and nothing could ever decode. These lock the
 * geometry down.
 */
class LumaRotationTest {

    /** A packed width x height plane whose value encodes its own position. */
    private fun plane(width: Int, height: Int): ByteArray =
        ByteArray(width * height) { i -> (i % 251).toByte() }

    private fun at(data: ByteArray, x: Int, rowStride: Int, y: Int): Int =
        data[y * rowStride + x].toInt() and 0xFF

    @Test
    fun `tighten drops the row padding`() {
        val width = 4
        val height = 3
        val stride = 6
        val padded = ByteArray(stride * height)
        for (y in 0 until height) {
            for (x in 0 until width) padded[y * stride + x] = (y * width + x).toByte()
            // Padding left as noise, which must not survive.
            for (x in width until stride) padded[y * stride + x] = 99
        }

        val packed = LumaRotation.tighten(padded, stride, width, height)

        assertEquals(width * height, packed.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                assertEquals(y * width + x, packed[y * width + x].toInt())
            }
        }
    }

    @Test
    fun `a quarter turn swaps the axes`() {
        val width = 4
        val height = 3
        val src = plane(width, height)

        val out = LumaRotation.rotate90(src, width, width, height)

        // Out is height wide and width tall.
        assertEquals(width * height, out.size)
        // The top-left of the source ends up top-right of the result.
        assertEquals(at(src, 0, width, 0), at(out, height - 1, height, 0))
        // The bottom-right of the source ends up bottom-left.
        assertEquals(
            at(src, width - 1, width, height - 1),
            at(out, 0, height, width - 1),
        )
    }

    @Test
    fun `four quarter turns come back to the start`() {
        // A quarter turn is not its own inverse — two of them are a half turn.
        // Four is the round trip, and each pass swaps the axes.
        val width = 5
        val height = 3
        var round = plane(width, height)
        var w = width
        var h = height
        repeat(4) {
            round = LumaRotation.rotate90(round, w, w, h)
            val swap = w
            w = h
            h = swap
        }

        assertArrayEquals(plane(width, height), round)
    }

    @Test
    fun `a half turn is its own inverse`() {
        val width = 4
        val height = 3
        val src = plane(width, height)

        val round = LumaRotation.rotate180(
            LumaRotation.rotate180(src, width, height),
            width, height,
        )

        assertArrayEquals(src, round)
    }

    @Test
    fun `two quarter turns equal a half turn`() {
        val width = 4
        val height = 3
        val src = plane(width, height)

        val twice = LumaRotation.rotate90(
            LumaRotation.rotate90(src, width, width, height),
            height, height, width,
        )

        assertArrayEquals(LumaRotation.rotate180(src, width, height), twice)
    }

    @Test
    fun `a three quarter turn is the inverse of a quarter turn`() {
        val width = 4
        val height = 3
        val src = plane(width, height)

        val three = LumaRotation.rotate270(src, width, width, height)
        val back = LumaRotation.rotate90(three, height, height, width)

        assertArrayEquals(src, back)
    }

    @Test
    fun `every pixel survives a full turn`() {
        val width = 6
        val height = 4
        val src = plane(width, height)

        var current = LumaRotation.rotate90(src, width, width, height)
        current = LumaRotation.rotate90(current, height, height, width)
        current = LumaRotation.rotate90(current, width, width, height)
        current = LumaRotation.rotate90(current, height, height, width)

        assertArrayEquals(src, current)
    }

    @Test
    fun `dispatch by degrees matches the explicit rotations`() {
        val width = 4
        val height = 3
        val stride = 5
        val padded = ByteArray(stride * height)
        for (y in 0 until height) {
            for (x in 0 until width) padded[y * stride + x] = (y * width + x).toByte()
        }

        val ninety = LumaRotation.rotate(padded, stride, width, height, 90)
        assertArrayEquals(LumaRotation.rotate90(padded, stride, width, height), ninety)

        // 450 and 90 are the same turn.
        assertArrayEquals(
            LumaRotation.rotate(padded, stride, width, height, 90),
            LumaRotation.rotate(padded, stride, width, height, 450),
        )

        // A negative angle is normalised rather than falling through.
        assertArrayEquals(
            LumaRotation.rotate(padded, stride, width, height, 90),
            LumaRotation.rotate(padded, stride, width, height, -270),
        )
    }

    @Test
    fun `dimensions swap only for quarter turns`() {
        assertEquals(4 to 3, LumaRotation.sizeAfterRotation(4, 3, 0))
        assertEquals(3 to 4, LumaRotation.sizeAfterRotation(4, 3, 90))
        assertEquals(4 to 3, LumaRotation.sizeAfterRotation(4, 3, 180))
        assertEquals(3 to 4, LumaRotation.sizeAfterRotation(4, 3, 270))
    }

    @Test
    fun `inverting twice is the identity`() {
        val src = plane(3, 2)
        assertArrayEquals(src, LumaRotation.invert(LumaRotation.invert(src)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a stride narrower than the image is rejected`() {
        LumaRotation.tighten(ByteArray(6), rowStride = 2, width = 4, height = 2)
    }
}