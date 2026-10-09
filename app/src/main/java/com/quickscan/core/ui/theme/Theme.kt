package com.quickscan.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Entry point for the visual system. Accent and text scale are inputs rather
 * than constants, so the Settings screen re-themes the whole app by changing
 * two arguments here.
 */
@Composable
fun QuickScanTheme(
    darkTheme: Boolean = false,
    accent: Accent = Accent.Ocean,
    largerText: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) darkPalette(accent) else lightPalette(accent)
    val text = qsText(if (largerText) 1.15f else 1f)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalQsPalette provides palette,
        LocalQsText provides text,
    ) {
        MaterialTheme(
            colorScheme = palette.toMaterialScheme(darkTheme),
            typography = Typography().toMaterialTypography(text),
            content = content,
        )
    }
}

private fun QsPalette.toMaterialScheme(dark: Boolean) =
    if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = accentOn,
            secondary = accentTintInk,
            onSecondary = accentTint,
            background = bg,
            onBackground = ink,
            surface = surface,
            onSurface = ink,
            surfaceVariant = surface,
            onSurfaceVariant = inkMuted,
            outline = hairline,
            outlineVariant = divider,
            error = danger,
            onError = accentOn,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = accentOn,
            secondary = accentTintInk,
            onSecondary = accentTint,
            background = bg,
            onBackground = ink,
            surface = surface,
            onSurface = ink,
            surfaceVariant = surface,
            onSurfaceVariant = inkMuted,
            outline = hairline,
            outlineVariant = divider,
            error = danger,
            onError = accentOn,
        )
    }

/** Readable access to the tokens from any composable. */
object QsTheme {
    val palette: QsPalette
        @Composable get() = LocalQsPalette.current
    val text: QsText
        @Composable get() = LocalQsText.current
}

private fun Typography.toMaterialTypography(text: QsText) = Typography(
    displaySmall = text.display32,
    headlineLarge = text.display30,
    headlineMedium = text.title26,
    headlineSmall = text.title21,
    titleLarge = text.title18,
    titleMedium = text.row15,
    titleSmall = text.chip13,
    bodyLarge = text.body16,
    bodyMedium = text.body15,
    bodySmall = text.rowSub12,
    labelLarge = text.nav16,
    labelMedium = text.chip13,
    labelSmall = text.tabLabel10,
)