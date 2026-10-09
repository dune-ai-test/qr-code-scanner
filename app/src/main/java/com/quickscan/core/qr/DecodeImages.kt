package com.quickscan.core.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageProxy

/**
 * Turning a captured frame or a picked photo into something ZXing can chew on.
 *
 * Both entry points — the shutter button and the photo picker — go through
 * here, so manual and automatic scanning decode exactly the same way.
 */
object DecodeImages {

    /**
     * Long edge, in pixels, that an image is sampled down to. A modern capture
     * is 12MP or more; handing that to ZXing means a 50MB int array plus its
     * binarizer buffers, which is slow enough to look like a hang. No code needs
     * that much resolution.
     */
    const val MAX_DECODE_EDGE = 1600

    /** Loads a picked photo, sampled down to [MAX_DECODE_EDGE]. */
    fun fromUri(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }.getOrNull()
    }

    /**
     * A shutter capture arrives as a single-plane JPEG inside the [ImageProxy].
     * The bounds are read first and the real decode runs at the sampled size,
     * so a 12MP capture never has to exist as a 48MB bitmap.
     */
    fun fromCapture(image: ImageProxy): Bitmap? {
        if (image.format != android.graphics.ImageFormat.JPEG) return null
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }


    private fun sampleSizeFor(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var longEdge = maxOf(width, height)
        while (longEdge / 2 >= MAX_DECODE_EDGE) {
            longEdge /= 2
            sample *= 2
        }
        return sample
    }
}
