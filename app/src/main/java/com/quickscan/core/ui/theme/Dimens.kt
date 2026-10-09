package com.quickscan.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing, radii and fixed chrome metrics. The mockups are laid out on a 390pt
 * canvas, so these values transfer one-for-one; Compose's own 4/8 dp grid covers
 * everything else.
 */
object Space {
    val xs = 8.dp
    val sm = 10.dp
    val md = 12.dp
    val lg = 14.dp
    val xl = 16.dp
    val xxl = 18.dp
    val x2xl = 20.dp
    val x3xl = 22.dp
    val section = 24.dp
    val x4xl = 26.dp
    val x5xl = 28.dp
    val x6xl = 30.dp
    val hero = 36.dp
}

object Radius {
    val xs = 12.dp
    val sm = 13.dp
    val md = 14.dp
    val lg = 16.dp
    val lgPlus = 17.dp
    val xl = 18.dp
    val xlPlus = 20.dp
    val xxl = 22.dp
    val hero = 28.dp
    val capsule = 31.dp
    val pill = 1000.dp
}

/** Fixed heights shared by more than one screen. */
object Chrome {
    val statusBar = 62.dp
    val navBar = 56.dp
    val tabBar = 62.dp
    val tabBarWidth = 358.dp
    val navButton = 36.dp
}

/** Metrics for the repeated row / tile / control shapes. */
object Metrics {
    val listRowHeight = 64.dp
    val settingRowHeight = 62.dp
    val detailRowHeight = 52.dp
    val chipHeight = 36.dp
    val buttonHeight = 54.dp
    val iconTile = 42.dp
    val iconTileCompact = 38.dp
    val statTile = 76.dp
    val heroAvatar = 96.dp
    val profileAvatar = 56.dp
    val emptyRing = 132.dp
    val switchTrack = 51.dp
    val switchTrackHeight = 31.dp
    val switchKnob = 27.dp
}