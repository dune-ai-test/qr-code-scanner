package com.quickscan.feature.scanner

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hosts the CameraX preview surface and forwards its surface provider to
 * [CameraController], which binds only once both a lifecycle owner and a
 * surface are available.
 */
@Composable
fun CameraPreview(
    controller: CameraController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )

    DisposableEffect(previewView) {
        controller.surfaceProvider = previewView.surfaceProvider
        onDispose { controller.surfaceProvider = null }
    }
}