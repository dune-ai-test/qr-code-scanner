package com.quickscan.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.quickscan.R

/**
 * The product typeface is Geist (Vercel, SIL Open Font License 1.1), a
 * neo-grotesque with the same skeleton as the platform UI face, so the
 * mockups' metrics hold.
 *
 * The four weights the type scale uses are vendored under `res/font`;
 * attribution is in `docs/ATTRIBUTION.md`.
 */
object AppTypeface {
    val family: FontFamily = FontFamily(
        Font(R.font.geist_regular, FontWeight.Normal),
        Font(R.font.geist_medium, FontWeight.Medium),
        Font(R.font.geist_semibold, FontWeight.SemiBold),
        Font(R.font.geist_bold, FontWeight.Bold),
    )
}