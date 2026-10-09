package com.quickscan.core.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Turns real content into a scannable QR matrix, and a matrix into a bitmap for
 * saving or sharing. Both directions are local — ZXing never leaves the device.
 */
object QrEncoder {

    /**
     * @param quietZone light modules around the code, as scanners expect.
     * @param errorCorrection M recovers roughly 7% of code, the level the
     *   create screen defaults to because printed codes get scuffed.
     */
    fun encode(
        content: String,
        quietZone: Int = 2,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
    ): BitMatrix? = runCatching {
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            MIN_RENDER_PX,
            MIN_RENDER_PX,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to errorCorrection,
                EncodeHintType.MARGIN to quietZone,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
    }.getOrNull()

    /** Rasterises [matrix] as [sizePx] square pixels. */
    fun render(
        matrix: BitMatrix,
        sizePx: Int,
        foreground: Int = Color.BLACK,
        background: Int = Color.WHITE,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background)
        val scale = sizePx.toFloat() / matrix.width
        val paint = android.graphics.Paint().apply { color = foreground }
        for (y in 0 until matrix.height) {
            var runStart = -1
            for (x in 0 until matrix.width) {
                val on = matrix[x, y]
                if (on && runStart < 0) runStart = x
                if (!on && runStart >= 0) {
                    canvas.drawRect(
                        runStart * scale,
                        y * scale,
                        x * scale,
                        (y + 1) * scale,
                        paint,
                    )
                    runStart = -1
                }
            }
            if (runStart >= 0) {
                canvas.drawRect(
                    runStart * scale,
                    y * scale,
                    matrix.width * scale,
                    (y + 1) * scale,
                    paint,
                )
            }
        }
        return bitmap
    }

    private const val MIN_RENDER_PX = 512
}