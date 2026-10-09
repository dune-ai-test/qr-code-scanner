package com.quickscan.data.barcode

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

data class DecodedCode(
    val text: String,
    val formatName: String,
)

/**
 * On-device decoding for camera frames and gallery images. ZXing is pure Java
 * and makes no network calls, which is what keeps the whole flow local.
 *
 * Deliberately stateless. A ZXing reader carries mutable per-image state and is
 * not thread-safe, and this class is a singleton reached from two places: the
 * CameraX analysis executor for every frame, and the main thread when a
 * gallery image is decoded. Sharing one reader across those threads silently
 * corrupts it and nothing ever decodes, so each call builds its own reader.
 */
class ZxingDecoder {

    /** Decodes the luminance plane of a YUV camera frame. */
    fun decode(image: ImageProxy): DecodedCode? {
        if (image.format != ImageFormat.YUV_420_888 &&
            image.format != ImageFormat.YUV_422_888 &&
            image.format != ImageFormat.YUV_444_888
        ) {
            return null
        }

        val plane = image.planes[0]
        val rowStride = plane.rowStride
        val width = image.width
        val height = image.height

        // The plane buffer can be padded relative to width, so copy exactly one
        // stride per row and leave any tail zeroed, rather than letting a short
        // final row shift every row after it.
        val packed = ByteArray(rowStride * height)
        val buffer = plane.buffer
        buffer.rewind()
        var row = 0
        while (row < height && buffer.remaining() >= rowStride) {
            buffer.get(packed, row * rowStride, rowStride)
            row++
        }

        val degrees = image.imageInfo.rotationDegrees
        val rotated = rotate(packed, rowStride, width, height, degrees)
        val outWidth = if (degrees % 180 == 0) width else height
        val outHeight = if (degrees % 180 == 0) height else width

        // Decode the centre square the reticle covers. It is a fraction of the
        // frame, so this is both faster and more forgiving of a code that only
        // partly fills the viewfinder.
        val side = (minOf(outWidth, outHeight) * CENTRE_FRACTION).toInt() and 1.inv()
        val left = (outWidth - side) / 2
        val top = (outHeight - side) / 2

        return decodeRegion(rotated, outWidth, outHeight, left, top, side)
            // Light-on-dark codes are common on printed labels and screens.
            ?: decodeRegion(invert(rotated), outWidth, outHeight, left, top, side)
            // A code near the edge of the frame is missed by the centre crop.
            ?: decodeLuminance(rotated, outWidth, outHeight)
    }

    private fun decodeRegion(
        data: ByteArray,
        dataWidth: Int,
        dataHeight: Int,
        left: Int,
        top: Int,
        side: Int,
    ): DecodedCode? {
        if (side <= 0 || left + side > dataWidth || top + side > dataHeight) return null
        val source = PlanarYUVLuminanceSource(
            data, dataWidth, dataHeight, left, top, side, side, false,
        )
        return decodeNodes(BinaryBitmap(HybridBinarizer(source)))
    }

    /** Decodes a still image, e.g. one picked from the photo library. */
    fun decode(bitmap: Bitmap): DecodedCode? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        return decodeNodes(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels))))
            ?: decodeLuminance(toLuminance(pixels, width, height), width, height)
    }

    private fun decodeLuminance(data: ByteArray, width: Int, height: Int): DecodedCode? {
        val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
        return decodeNodes(BinaryBitmap(HybridBinarizer(source)))
    }

    private fun decodeNodes(bitmap: BinaryBitmap): DecodedCode? {
        val reader = MultiFormatReader().apply { setHints(HINTS) }
        return runCatching { reader.decode(bitmap).toDecoded() }.getOrNull()
    }

    /** BT.601 luma, matching what the camera pipeline feeds the decoder. */
    private fun toLuminance(pixels: IntArray, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            out[i] = ((r * 66 + g * 151 + b * 29) shr 8).toByte()
        }
        return out
    }

    private fun com.google.zxing.Result.toDecoded() = DecodedCode(
        text = text,
        formatName = barcodeFormat.name,
    )

    /** Rotates a row-stride luminance buffer into a tightly packed one. */
    private fun rotate(
        src: ByteArray,
        rowStride: Int,
        width: Int,
        height: Int,
        degrees: Int,
    ): ByteArray = when (((degrees % 360) + 360) % 360) {
        0 -> tighten(src, rowStride, width, height)
        90 -> rotate90(src, rowStride, width, height)
        180 -> rotate180(tighten(src, rowStride, width, height), width, height)
        else -> rotate270(src, rowStride, width, height)
    }

    private fun tighten(src: ByteArray, rowStride: Int, width: Int, height: Int): ByteArray =
        ByteArray(width * height).also { out ->
            for (y in 0 until height) {
                System.arraycopy(src, y * rowStride, out, y * width, width)
            }
        }

    private fun rotate90(src: ByteArray, rowStride: Int, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                out[x * height + (height - 1 - y)] = src[y * rowStride + x]
            }
        }
        return out
    }

    private fun rotate180(src: ByteArray, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                out[(height - 1 - y) * width + (width - 1 - x)] = src[y * width + x]
            }
        }
        return out
    }

    private fun rotate270(src: ByteArray, rowStride: Int, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                out[(width - 1 - x) * height + y] = src[y * rowStride + x]
            }
        }
        return out
    }

    private fun invert(src: ByteArray): ByteArray =
        ByteArray(src.size) { i -> (255 - (src[i].toInt() and 0xFF)).toByte() }

    private companion object {
        /** Share of the short edge the reticle covers. */
        const val CENTRE_FRACTION = 0.78f

        val HINTS: Map<DecodeHintType, Any> = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.AZTEC,
                BarcodeFormat.PDF_417,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR,
            ),
            DecodeHintType.TRY_HARDER to true,
        )
    }
}