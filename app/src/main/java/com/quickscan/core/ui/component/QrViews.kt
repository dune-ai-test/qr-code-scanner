package com.quickscan.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickscan.core.qr.QrRenderer
import com.quickscan.core.qr.QrStyle
import com.quickscan.core.qr.QrPlaceholder

/**
 * The one place a QR code is drawn on screen.
 *
 * A code takes a [QrStyle] rather than two colours because the style is part
 * of the code: a saved one has to render the way it was made, and the card
 * behind it — not the code — is what follows the theme.
 *
 * The matrix is cached per payload and style, so recomposition does not re-run
 * the encoder.
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    style: QrStyle = QrStyle.DEFAULT,
) {
    val matrix = remember(content, style) { QrRenderer.encode(content, style) }
    Canvas(modifier) {
        val encoded = matrix ?: return@Canvas
        QrRenderer.draw(
            drawContext.canvas.nativeCanvas,
            encoded,
            style,
            size.width.toInt(),
        )
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
