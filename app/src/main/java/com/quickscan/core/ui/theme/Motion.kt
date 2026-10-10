package com.quickscan.core.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether looping animations should run.
 *
 * The sweep line and the empty-state shimmer repeat forever, which is exactly
 * what the system's animation scale is for. When the user has turned
 * animations off, those become a single static frame rather than a loop.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}