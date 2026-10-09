package com.quickscan.core.qr

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Getting a created code off the device as an image, which is what people
 * actually want to send: a PNG in the chat, not a wall of encoded text.
 *
 * Everything here is local. A share sheet hands the file to another app; the
 * app never uploads anything itself.
 */
object QrExporter {

    private const val SHARE_DIR = "shared-qr"
    private const val FILE_NAME = "quick-scan-code.png"

    /** Writes the bitmap into the gallery and returns its MediaStore URI. */
    fun saveToGallery(context: Context, bitmap: Bitmap): Result<Uri> = runCatching {
        val name = "QuickScan ${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, MIME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QuickScan")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore refused the insert")

        resolver.openOutputStream(uri).use { output ->
            checkNotNull(output) { "could not open $uri for writing" }
            check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) {
                "PNG encode failed"
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    }

    /**
     * A share sheet carrying the image itself. The bitmap goes to the cache
     * directory and is exposed through a FileProvider, so no storage
     * permission is involved.
     */
    fun shareIntent(context: Context, bitmap: Bitmap, chooserTitle: String): Intent {
        val file = writeToCache(context, bitmap)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { send ->
            Intent.createChooser(send, chooserTitle)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun writeToCache(context: Context, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val file = File(dir, FILE_NAME)
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) {
                "PNG encode failed"
            }
        }
        return file
    }

    private const val MIME = "image/png"

    /** PNG is lossless, so the quality argument is ignored by the encoder. */
    private const val PNG_QUALITY = 100
}
