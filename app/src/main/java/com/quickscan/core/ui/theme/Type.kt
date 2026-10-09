package com.quickscan.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * The type scale lifted straight from the mockups: size, leading and tracking
 * per role. `largerText` in Settings scales every size by 1.15 at the theme
 * boundary rather than at each call site.
 */
@Immutable
data class QsText(
    val display32: TextStyle,
    val display30: TextStyle,
    val title26: TextStyle,
    val title21: TextStyle,
    val title18: TextStyle,
    val nav16: TextStyle,
    val body16: TextStyle,
    val row15: TextStyle,
    val body15: TextStyle,
    val body14: TextStyle,
    val chip13: TextStyle,
    val rowSub12: TextStyle,
    val label11: TextStyle,
    val tabLabel10: TextStyle,
)

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

internal fun qsText(scale: Float = 1f): QsText {
    fun size(value: Int) = (value * scale).sp
    fun style(
        size: Int,
        lineHeight: Int,
        weight: FontWeight,
        tracking: Double = 0.0,
    ) = TextStyle(
        fontFamily = AppTypeface.family,
        fontWeight = weight,
        fontSize = size(size),
        lineHeight = size(lineHeight),
        letterSpacing = tracking.sp,
        lineHeightStyle = Trim,
    )

    return QsText(
        display32 = style(32, 37, FontWeight.Bold, -0.8),
        display30 = style(30, 35, FontWeight.Bold, -0.7),
        title26 = style(26, 32, FontWeight.SemiBold, -0.5),
        title21 = style(21, 27, FontWeight.SemiBold, -0.4),
        title18 = style(18, 24, FontWeight.SemiBold, -0.2),
        nav16 = style(16, 21, FontWeight.SemiBold),
        body16 = style(16, 22, FontWeight.Normal, -0.1),
        row15 = style(15, 20, FontWeight.Medium),
        body15 = style(15, 21, FontWeight.Normal),
        body14 = style(14, 20, FontWeight.Normal),
        chip13 = style(13, 18, FontWeight.Medium),
        rowSub12 = style(12, 16, FontWeight.Normal),
        label11 = style(11, 14, FontWeight.SemiBold, 0.8),
        tabLabel10 = style(10, 13, FontWeight.Medium, 0.1),
    )
}

val LocalQsText = staticCompositionLocalOf { qsText() }