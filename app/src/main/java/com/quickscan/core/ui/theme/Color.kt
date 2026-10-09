package com.quickscan.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour in QuickScan lives here. The mockups carry literal hex values on
 * each node; those values are the source of truth for the fields below, so a
 * change to a token here re-themes the entire product.
 */
@Immutable
data class QsPalette(
    val bg: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val divider: Color,
    val hairline: Color,
    val accent: Color,
    val accentOn: Color,
    val accentTint: Color,
    val accentTintInk: Color,
    val successSurface: Color,
    val successInk: Color,
    val successIcon: Color,
    val warnSurface: Color,
    val warnInk: Color,
    val warnIcon: Color,
    val danger: Color,
    val scene: Color,
    val sceneInk: Color,
    val qrInk: Color,
    val qrPaper: Color,
    val live: Color,
    val frosted: Color,
    val frostedOutline: Color,
    val shadow: Color,
)

/** Accent presets offered on the Settings screen. */
enum class Accent(val label: String) {
    Ocean("Ocean"),
    Indigo("Indigo"),
    Moss("Moss"),
    Clay("Clay"),
}

private data class AccentSet(
    val accent: Color,
    val accentOn: Color,
    val tint: Color,
    val tintInk: Color,
    /** Dark mode needs its own tint; the light one is near-white. */
    val tintDark: Color,
    val tintInkDark: Color,
)

/** The swatch colour shown for this accent in the Settings picker. */
fun Accent.swatch(): Color = Set().accent

/** The dot colour drawn on top of the selected swatch. */
fun Accent.swatchOn(): Color = Set().accentOn

private fun Accent.Set() = when (this) {
    Accent.Ocean -> AccentSet(
        accent = Color(0xFF3D8FD1),
        accentOn = Color(0xFFFFFFFF),
        tint = Color(0xFFE5F0F9),
        tintInk = Color(0xFF2F77B4),
        tintDark = Color(0xFF17334A),
        tintInkDark = Color(0xFF8CC4EC),
    )

    Accent.Indigo -> AccentSet(
        accent = Color(0xFF5B5BD6),
        accentOn = Color(0xFFFFFFFF),
        tint = Color(0xFFEBEBFA),
        tintInk = Color(0xFF4A4AC4),
        tintDark = Color(0xFF26265A),
        tintInkDark = Color(0xFFA6A6F2),
    )

    Accent.Moss -> AccentSet(
        accent = Color(0xFF3F8F5B),
        accentOn = Color(0xFFFFFFFF),
        tint = Color(0xFFE4F1EA),
        tintInk = Color(0xFF2E7A4A),
        tintDark = Color(0xFF16341F),
        tintInkDark = Color(0xFF86D2A4),
    )

    Accent.Clay -> AccentSet(
        accent = Color(0xFFC4632B),
        accentOn = Color(0xFFFFFFFF),
        tint = Color(0xFFFAEFE2),
        tintInk = Color(0xFFB25A24),
        tintDark = Color(0xFF3A2314),
        tintInkDark = Color(0xFFE8A96C),
    )
}

internal fun lightPalette(preset: Accent) = with(preset.Set()) {
    QsPalette(
        bg = Color(0xFFFFFFFF),
        surface = Color(0xFFF4F5F7),
        surfaceElevated = Color(0xFFFFFFFF),
        ink = Color(0xFF111318),
        inkMuted = Color(0xFF5A606B),
        inkFaint = Color(0xFF8B919B),
        divider = Color(0x0A000000),
        hairline = Color(0xFFD6D9DE),
        accent = accent,
        accentOn = accentOn,
        accentTint = tint,
        accentTintInk = tintInk,
        successSurface = Color(0xFFE4F4EA),
        successInk = Color(0xFF1E6B3C),
        successIcon = Color(0xFF2E9E5B),
        warnSurface = Color(0xFFFAEFE2),
        warnInk = Color(0xFF8A511F),
        warnIcon = Color(0xFFD9822B),
        danger = Color(0xFFD94A4A),
        scene = Color(0xFF0C0D10),
        sceneInk = Color(0xFFFFFFFF),
        qrInk = Color(0xFF1A1E24),
        qrPaper = Color(0xFFFFFFFF),
        live = Color(0xFF4ADE80),
        frosted = Color(0xD9FFFFFF),
        frostedOutline = Color(0x80FFFFFF),
        shadow = Color(0x1F0A0A0A),
    )
}

internal fun darkPalette(preset: Accent) = with(preset.Set()) {
    QsPalette(
        bg = Color(0xFF0B0C0F),
        surface = Color(0xFF16181D),
        surfaceElevated = Color(0xFF1E2127),
        ink = Color(0xFFF2F3F5),
        inkMuted = Color(0xFFA8AEB8),
        inkFaint = Color(0xFF767C86),
        divider = Color(0x14FFFFFF),
        hairline = Color(0xFF2A2E35),
        accent = accent,
        accentOn = Color(0xFF0B0C0F),
        accentTint = tintDark,
        accentTintInk = tintInkDark,
        successSurface = Color(0xFF16281E),
        successInk = Color(0xFF8FD6AC),
        successIcon = Color(0xFF4ADE80),
        warnSurface = Color(0xFF2B2118),
        warnInk = Color(0xFFE0B183),
        warnIcon = Color(0xFFE0A15F),
        danger = Color(0xFFE96A6A),
        scene = Color(0xFF000000),
        sceneInk = Color(0xFFFFFFFF),
        qrInk = Color(0xFFEDEFF2),
        qrPaper = Color(0xFF16181D),
        live = Color(0xFF4ADE80),
        frosted = Color(0xE616181D),
        frostedOutline = Color(0x1AFFFFFF),
        shadow = Color(0x66000000),
    )
}

val LocalQsPalette = staticCompositionLocalOf { lightPalette(Accent.Ocean) }