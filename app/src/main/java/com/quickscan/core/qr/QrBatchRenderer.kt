package com.quickscan.core.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.quickscan.core.qr.BatchPayloads.Item

/**
 * Draws a sheet of codes, one per line of what was pasted.
 *
 * The codes are rendered through [QrRenderer] one at a time and blitted, rather
 * than sharing one canvas. `QrRenderer.draw` fills the canvas with the style's
 * background, which for a sheet would paint over everything already there.
 * Rendering individually costs one small bitmap at a time — about 0.9MB that
 * is collectable immediately — and buys an exact match with the single-code
 * export, which is worth more than the allocation.
 */
object QrBatchRenderer {

    /** Printable sheets are light; a code on a dark background does not scan. */
    private const val SHEET_BACKGROUND = Color.WHITE

    private const val LABEL_TEXT_PX = 26f
    private const val LABEL_LINE_PX = 32f
    private const val LABEL_COLOUR = 0xFF111318.toInt()
    private const val MISSING_COLOUR = 0xFFD94A4A.toInt()

    /**
     * @return null when there is nothing to draw.
     */
    fun render(
        items: List<Item>,
        style: QrStyle = QrStyle.DEFAULT,
    ): Bitmap? {
        if (items.isEmpty()) return null
        val options = QrContactSheet.optionsFor(items.size)
        val bitmap = Bitmap.createBitmap(options.width, options.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(SHEET_BACKGROUND)

        // A dark-mode preview must not produce a dark poster.
        val sheetStyle = style.copy(background = SHEET_BACKGROUND)
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LABEL_COLOUR
            textSize = LABEL_TEXT_PX
            textAlign = Paint.Align.CENTER
        }
        val missing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MISSING_COLOUR
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val placements = QrContactSheet.placements(options)
        for ((index, item) in items.withIndex()) {
            val place = placements[index]
            val centreX = place.labelLeft + place.labelWidth / 2f

            val code = QrRenderer.render(item.content, place.codeSize, sheetStyle)
            if (code != null) {
                canvas.drawBitmap(code, place.codeLeft.toFloat(), place.codeTop.toFloat(), null)
            } else {
                // Overlong content cannot fit a QR code. A visible empty cell
                // says which line failed; a silently missing one does not.
                val inset = place.codeSize * 0.06f
                canvas.drawRoundRect(
                    RectF(
                        place.codeLeft + inset,
                        place.codeTop + inset,
                        place.codeLeft + place.codeSize - inset,
                        place.codeTop + place.codeSize - inset,
                    ),
                    16f,
                    16f,
                    missing,
                )
            }

            val lines = wrap(item.label, maxChars = 28)
            lines.take(MAX_LABEL_LINES).forEachIndexed { line, text ->
                canvas.drawText(
                    text,
                    centreX,
                    place.labelTop + LABEL_LINE_PX * (line + 1),
                    label,
                )
            }
        }
        return bitmap
    }

    /** Two lines is what the cell is sized for; a third is better than a clip. */
    private const val MAX_LABEL_LINES = 2

    private fun wrap(text: String, maxChars: Int): List<String> {
        val single = text.replace('\n', ' ').trim()
        if (single.isEmpty()) return listOf("")
        if (single.length <= maxChars) return listOf(single)
        val cut = single.take(maxChars).lastIndexOf(' ')
        val head = if (cut > maxChars / 2) single.take(cut) else single.take(maxChars)
        return listOf(head.trimEnd(), single.drop(cut.coerceAtLeast(0)).take(maxChars).trimStart())
    }
}