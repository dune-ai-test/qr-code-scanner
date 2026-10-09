package com.quickscan.data.barcode

import android.graphics.Bitmap
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
 */
class ZxingDecoder {

    private val reader = MultiFormatReader().apply { setHints(HINTS) }

    /** Decodes the luminance plane of a YUV_420_888 camera frame. */
    fun decode(image: ImageProxy): DecodedCode? {
        if (image.format != android.graphics.ImageFormat.YUV_420_888 &&
            image.format != android.graphics.ImageFormat.YUV_422_888 &&
            image.format != android.graphics.ImageFormat.YUV_444_888
        ) {
            return null
        }

        val plane = image.planes[0]
        val rowStride = plane.rowStride
        val width = image.width
        val height = image.height

        // The plane buffer can be padded or cropped relative to width*height, so
        // read exactly the rows and columns we need rather than trusting limit().
        val packed = ByteArray(rowStride * height)
        val buffer = plane.buffer
        buffer.rewind()
        var copied = 0
        while (copied < height) {
            val chunk = minOf(buffer.remaining(), rowStride)
            if (chunk <= 0) break
            buffer.get(packed, copied, chunk)
            copied += chunk
        }

        val degrees = image.imageInfo.rotationDegrees
        val rotated = rotate(packed, rowStride, width, height, degrees)
        val outWidth = if (degrees % 180 == 0) width else height
        val outHeight = if (degrees % 180 == 0) height else width

        return decodeLuminance(rotated, outWidth, outHeight)
            ?: decodeLuminance(invert(rotated), outWidth, outHeight)
    }

    private fun decodeLuminance(data: ByteArray, width: Int, height: Int): DecodedCode? {
        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false,
        )
        return runCatching {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).toDecoded()
        }.getOrNull()
    }

    /** Decodes a still image, e.g. one picked from the photo library. */
    fun decode(bitmap: Bitmap): DecodedCode? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val luminance = ByteArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // BT.601 luma, matching what the camera pipeline feeds the decoder.
            luminance[i] = ((r * 66 + g * 151 + b * 29) shr 8).toByte()
        }

        val source = RGBLuminanceSource(width, height, pixels)
        val fromPixels = runCatching {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).toDecoded()
        }.getOrNull()
        if (fromPixels != null) return fromPixels

        val grey = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        return runCatching {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(grey))).toDecoded()
        }.getOrNull()
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