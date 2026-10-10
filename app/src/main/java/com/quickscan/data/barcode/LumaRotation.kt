package com.quickscan.data.barcode

/**
 * Rotating and packing the camera's luminance plane.
 *
 * Separated from the decoder because this is the part that quietly produces a
 * half-black image if it is wrong, and because it is pure array work that can
 * be unit tested without a device.
 *
 * Everything here works on a signed [ByteArray] of row-stride width.
 */
object LumaRotation {

    /** Copies a padded plane into a tightly packed one, row by row. */
    fun tighten(src: ByteArray, rowStride: Int, width: Int, height: Int): ByteArray {
        require(rowStride >= width) { "rowStride $rowStride is narrower than width $width" }
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            System.arraycopy(src, y * rowStride, out, y * width, width)
        }
        return out
    }

    /**
     * Rotates a packed plane by [degrees], which must be a multiple of 90.
     *
     * A 90 degree turn swaps the axes: the result is [height] wide and [width]
     * tall, which is why callers must pass the swapped dimensions onwards.
     */
    fun rotate(
        src: ByteArray,
        rowStride: Int,
        width: Int,
        height: Int,
        degrees: Int,
    ): ByteArray = when (((degrees % 360) + 360) % 360) {
        0 -> tighten(src, rowStride, width, height)
        90 -> rotate90(src, rowStride, width, height)
        180 -> rotate180(tighten(src, rowStride, width, height), width, height)
        270 -> rotate270(src, rowStride, width, height)
        else -> tighten(src, rowStride, width, height)
    }

    /** Output is [height] wide and [width] tall. */
    fun rotate90(src: ByteArray, rowStride: Int, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            val from = y * rowStride
            for (x in 0 until width) {
                out[x * height + (height - 1 - y)] = src[from + x]
            }
        }
        return out
    }

    fun rotate180(src: ByteArray, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            val from = y * width
            val to = (height - 1 - y) * width
            for (x in 0 until width) {
                out[to + (width - 1 - x)] = src[from + x]
            }
        }
        return out
    }

    /** Output is [height] wide and [width] tall. */
    fun rotate270(src: ByteArray, rowStride: Int, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            val from = y * rowStride
            for (x in 0 until width) {
                out[(width - 1 - x) * height + y] = src[from + x]
            }
        }
        return out
    }

    fun invert(src: ByteArray): ByteArray =
        ByteArray(src.size) { i -> (255 - (src[i].toInt() and 0xFF)).toByte() }

    /** Dimensions after a rotation by [degrees]. */
    fun sizeAfterRotation(width: Int, height: Int, degrees: Int): Pair<Int, Int> =
        if (degrees % 180 == 0) width to height else height to width
}