package com.quickscan.core.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * The product typeface is Geist (Vercel, SIL Open Font License), a neo-grotesque
 * with the same skeleton as the platform UI face, so the mockups' metrics hold.
 *
 * To switch to the real family, drop `Geist-Regular.ttf`, `Geist-Medium.ttf`,
 * `Geist-SemiBold.ttf` and `Geist-Bold.ttf` into `app/src/main/res/font/` and
 * replace the body of [family] with:
 *
 * ```
 * FontFamily(
 *     Font(R.font.geist_regular, FontWeight.Normal),
 *     Font(R.font.geist_medium, FontWeight.Medium),
 *     Font(R.font.geist_semibold, FontWeight.SemiBold),
 *     Font(R.font.geist_bold, FontWeight.Bold),
 * )
 * ```
 *
 * The default keeps the build green with no vendored binaries.
 */
object AppTypeface {
    val family: FontFamily = FontFamily.Default
}