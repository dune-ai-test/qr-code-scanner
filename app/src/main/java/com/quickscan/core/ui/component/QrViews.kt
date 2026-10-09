package com.quickscan.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickscan.core.qr.QrEncoder
import com.quickscan.core.qr.QrRenderer
import com.quickscan.core.qr.QrStyle
import com.quickscan.core.qr.QrPlaceholder
import com.google.zxing.common.BitMatrix

/**
 * A real, scannable QR code for [content]. The matrix is cached per content, so
 * recomposition does not re-run the encoder.
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    foreground: Color,
    background: Color,
    quietZone: Int = 2,
) {
    val matrix = remember(content, quietZone) { QrEncoder.encode(content, quietZone) }
    Canvas(modifier) {
        drawRect(background)
        matrix?.let { drawMatrix(it, foreground) }
    }
}

/**
 * Decorative QR artwork for the onboarding hero and the scanner's sample poster.
 * [seed] selects which pattern is painted; equal seeds paint equal codes.
 */
@Composable
fun QrPlaceholderView(
    seed: Long,
    modifier: Modifier = Modifier,
    foreground: Color,
    moduleSize: Int = QrPlaceholder.DEFAULT_SIZE,
) {
    val grid = remember(seed, moduleSize) { QrPlaceholder.generate(seed, moduleSize) }
    Canvas(modifier) {
        val cell = size.minDimension / grid.size
        for (y in 0 until grid.size) {
            for (segment in grid.runsInRow(y)) {
                drawRect(
                    color = foreground,
                    topLeft = Offset(segment.first * cell, y * cell),
                    size = Size(segment.count() * cell, cell),
                )
            }
        }
    }
}

/**
 * A QR rendered with the creator's styling: coloured modules, rounded corners
 * and an optional logo. Uses the same renderer as the PNG export, so what the
 * preview shows is what gets shared.
 */
@Composable
fun QrStyledView(
    payload: String,
    style: QrStyle,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(payload, style.logo) { QrRenderer.encode(payload, style) }
    Canvas(modifier) {
        val m = matrix ?: return@Canvas
        QrRenderer.draw(this, m, style, size.width.toInt())
    }
}

/**
 * The four corner brackets the mockups wrap around hero codes and the
 * viewfinder. Drawn with round caps to match the reticle stroke.
 */
@Composable
fun QrCornerBrackets(
    color: Color,
    modifier: Modifier = Modifier,
    armLength: Dp = 32.dp,
    strokeWidth: Dp = 4.dp,
) {
    Canvas(modifier) {
        val arm = armLength.toPx()
        val w = size.width
        val h = size.height

        drawLine(color, Offset(0f, arm), Offset(0f, 0f), strokeWidth.toPx(), StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(arm, 0f), strokeWidth.toPx(), StrokeCap.Round)

        drawLine(color, Offset(w - arm, 0f), Offset(w, 0f), strokeWidth.toPx(), StrokeCap.Round)
        drawLine(color, Offset(w, 0f), Offset(w, arm), strokeWidth.toPx(), StrokeCap.Round)

        drawLine(color, Offset(0f, h - arm), Offset(0f, h), strokeWidth.toPx(), StrokeCap.Round)
        drawLine(color, Offset(0f, h), Offset(arm, h), strokeWidth.toPx(), StrokeCap.Round)

        drawLine(color, Offset(w - arm, h), Offset(w, h), strokeWidth.toPx(), StrokeCap.Round)
        drawLine(color, Offset(w, h), Offset(w, h - arm), strokeWidth.toPx(), StrokeCap.Round)
    }
}

private fun DrawScope.drawMatrix(matrix: BitMatrix, color: Color) {
    val cell = size.minDimension / matrix.width
    for (y in 0 until matrix.height) {
        var segmentStart = -1
        for (x in 0 until matrix.width) {
            val on = matrix[x, y]
            if (on && segmentStart < 0) segmentStart = x
            if (!on && segmentStart >= 0) {
                drawRect(
                    color = color,
                    topLeft = Offset(segmentStart * cell, y * cell),
                    size = Size((x - segmentStart) * cell, cell),
                )
                segmentStart = -1
            }
        }
        if (segmentStart >= 0) {
            drawRect(
                color = color,
                topLeft = Offset(segmentStart * cell, y * cell),
                size = Size((matrix.width - segmentStart) * cell, cell),
            )
        }
    }
}

/** Convenience wrapper that pins a real QR code to a fixed edge length. */
@Composable
fun SquareQr(
    edge: Dp,
    content: String,
    modifier: Modifier = Modifier,
    foreground: Color,
    background: Color,
    quietZone: Int = 2,
) {
    QrCodeView(
        content = content,
        modifier = modifier.size(edge),
        foreground = foreground,
        background = background,
        quietZone = quietZone,
    )
}