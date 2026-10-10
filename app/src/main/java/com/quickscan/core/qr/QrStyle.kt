package com.quickscan.core.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * How a created code looks. Deliberately restrained: a QR has to stay
 * scannable, so the surface stays high contrast and the decoration is limited
 * to shape and an optional logo punched out of the middle.
 */
data class QrStyle(
    /** Colour of the dark modules. */
    val foreground: Int = DEFAULT_FOREGROUND,
    /** Colour behind the code. */
    val background: Int = DEFAULT_BACKGROUND,
    /**
     * Corner rounding as a fraction of a module's edge: 0 is a hard square,
     * 0.5 is fully round. Values above ~0.45 start costing scannability.
     */
    val cornerRadius: Float = 0f,
    /** Optional mark drawn over the centre, with a cleared area behind it. */
    val logo: QrLogo? = null,
) {
    val isPlain: Boolean get() = cornerRadius == 0f && logo == null

    companion object {
        const val DEFAULT_FOREGROUND = 0xFF111318.toInt()
        const val DEFAULT_BACKGROUND = 0xFFFFFFFF.toInt()

        /** A code with a logo needs the extra redundancy to survive the hole. */
        val LOGO_ERROR_CORRECTION = ErrorCorrectionLevel.H

        val DEFAULT = QrStyle()
    }
}

/** The built-in marks that can sit in the middle of a code. */
enum class QrLogo {
    None,
    App,
    Link,
    Wifi,
    Contact,
    Plain,
}

/**
 * Renders a QR to a [Bitmap] with styling applied.
 *
 * The module grid is drawn as individual cells rather than merged runs so
 * corners can be rounded; a logo punches a cleared square out of the middle
 * and draws the mark inside it.
 */
object QrRenderer {

    /**
     * @param payload text to encode
     * @param sizePx edge length of the square bitmap
     * @param style colours, corner rounding and optional logo
     * @param quietZone light modules around the code; scanners expect some
     */
    fun render(
        payload: String,
        sizePx: Int,
        style: QrStyle = QrStyle.DEFAULT,
        quietZone: Int = QUIET_ZONE,
    ): Bitmap? {
        val matrix = encode(payload, style) ?: return null
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), matrix, style, sizePx)
        return bitmap
    }

    fun encode(payload: String, style: QrStyle = QrStyle.DEFAULT): BitMatrix? = runCatching {
        QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            MIN_RENDER_PX,
            MIN_RENDER_PX,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to
                    if (style.logo == null || style.logo == QrLogo.None) {
                        ErrorCorrectionLevel.M
                    } else {
                        QrStyle.LOGO_ERROR_CORRECTION
                    },
                EncodeHintType.MARGIN to QUIET_ZONE,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
    }.getOrNull()

    /** Draws an already-encoded matrix, so a live preview and an export agree. */
    fun draw(canvas: Canvas, matrix: BitMatrix, style: QrStyle, sizePx: Int) {
        canvas.drawColor(style.background)

        val modules = matrix.width
        val cell = sizePx.toFloat() / modules
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.foreground }

        val logoModules = style.logo.sizeInModules()
        val hole = logoHole(matrix, logoModules)

        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (!matrix[x, y]) continue
                if (hole != null && x in hole.left until hole.right &&
                    y in hole.top until hole.bottom
                ) {
                    continue
                }
                val left = x * cell
                val top = y * cell
                if (style.cornerRadius <= 0f) {
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                } else {
                    val radius = cell * style.cornerRadius.coerceAtMost(0.5f)
                    canvas.drawRoundRect(
                        RectF(left, top, left + cell, top + cell),
                        radius,
                        radius,
                        paint,
                    )
                }
            }
        }

        if (style.logo != null && hole != null && style.logo != QrLogo.None) {
            val bounds = RectF(
                hole.left * cell,
                hole.top * cell,
                hole.right * cell,
                hole.bottom * cell,
            )
            // Clear only the hole, not the whole canvas.
            canvas.drawRect(bounds, Paint().apply { color = style.background })
            val inset = bounds.width() * LOGO_INSET
            drawLogo(
                canvas,
                style.logo,
                Rect(
                    (bounds.left + inset).toInt(),
                    (bounds.top + inset).toInt(),
                    (bounds.right - inset).toInt(),
                    (bounds.bottom - inset).toInt(),
                ),
            )
        }
    }

    /**
     * A square of modules in the middle to clear, sized to the code and never
     * overlapping the three finder patterns.
     */
    private fun logoHole(matrix: BitMatrix, sizeModules: Int): Rect? {
        if (sizeModules <= 0) return null
        val span = matrix.width
        // Keep the hole clear of the 8-module-wide finder bands.
        val margin = 8
        val available = span - margin * 2
        if (available < sizeModules) return null

        val side = sizeModules.coerceAtMost(available)
        val inset = (span - side) / 2
        // Land on a module boundary, and make the side odd so it centres.
        return Rect(inset, inset, inset + side, inset + side)
    }

    private fun drawLogo(canvas: Canvas, logo: QrLogo, bounds: Rect) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = QrStyle.DEFAULT_FOREGROUND
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = QrStyle.DEFAULT_FOREGROUND
            style = Paint.Style.STROKE
            strokeWidth = bounds.width() * STROKE_FRACTION
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val w = bounds.width()

        when (logo) {
            QrLogo.None -> Unit

            QrLogo.Plain -> canvas.drawCircle(cx, cy, w * 0.18f, paint)

            QrLogo.App -> {
                // The app's own 4x4 module mark, reduced to three squares.
                val u = w * 0.14f
                canvas.drawRect(cx - u * 1.5f, cy - u * 1.5f, cx - u * 0.5f, cy - u * 0.5f, paint)
                canvas.drawRect(cx + u * 0.5f, cy - u * 1.5f, cx + u * 1.5f, cy - u * 0.5f, paint)
                canvas.drawRect(cx - u * 1.5f, cy + u * 0.5f, cx - u * 0.5f, cy + u * 1.5f, paint)
                canvas.drawRect(cx + u * 0.5f, cy + u * 0.5f, cx + u * 1.5f, cy + u * 1.5f, paint)
            }

            QrLogo.Link -> {
                stroke.strokeWidth = bounds.width() * 0.14f
                canvas.drawArc(
                    cx - w * 0.42f, cy - w * 0.26f, cx - w * 0.02f, cy + w * 0.26f,
                    -80f, 160f, false, stroke,
                )
                canvas.drawArc(
                    cx + w * 0.02f, cy - w * 0.26f, cx + w * 0.42f, cy + w * 0.26f,
                    100f, 160f, false, stroke,
                )
            }

            QrLogo.Wifi -> {
                stroke.strokeWidth = bounds.width() * 0.13f
                canvas.drawArc(
                    cx - w * 0.42f, cy - w * 0.16f, cx + w * 0.42f, cy + w * 0.68f,
                    215f, 110f, false, stroke,
                )
                canvas.drawArc(
                    cx - w * 0.26f, cy + w * 0.0f, cx + w * 0.26f, cy + w * 0.52f,
                    215f, 110f, false, stroke,
                )
                canvas.drawCircle(cx, cy + w * 0.3f, w * 0.07f, paint)
            }

            QrLogo.Contact -> {
                canvas.drawCircle(cx, cy - w * 0.18f, w * 0.17f, paint)
                val head = RectF(cx - w * 0.34f, cy + w * 0.06f, cx + w * 0.34f, cy + w * 0.5f)
                canvas.drawRoundRect(head, w * 0.18f, w * 0.18f, paint)
            }
        }
    }

    /** How many modules of the centre a logo clears. */
    private fun QrLogo?.sizeInModules(): Int = when (this) {
        null, QrLogo.None -> 0
        else -> LOGO_MODULES
    }

    private const val QUIET_ZONE = 2
    private const val MIN_RENDER_PX = 512
    private const val LOGO_MODULES = 13
    private const val LOGO_INSET = 0.06f
    private const val STROKE_FRACTION = 0.1f
}
