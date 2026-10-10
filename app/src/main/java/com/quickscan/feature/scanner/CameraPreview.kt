package com.quickscan.feature.scanner

import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hosts the CameraX preview surface and forwards its surface provider to
 * [CameraController], which binds only once both a lifecycle owner and a
 * surface are available.
 *
 * Pinch is handled here because the preview fills the whole viewfinder, so a
 * gesture on it is unambiguously a zoom rather than a scroll. The controller
 * clamps to what the lens supports, so a pinch past the end stops rather than
 * failing, and the applied ratio comes back through [onZoom] for the readout.
 */
@Composable
fun CameraPreview(
    controller: CameraController,
    onZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.pointerInput(controller) {
            detectTransformGestures { _, _, zoomChange, _ ->
                if (zoomChange <= 0f) return@detectTransformGestures
                onZoom(controller.setZoomRatio(controller.zoom.value.current * zoomChange))
            }
        },
    )

    DisposableEffect(previewView) {
        controller.surfaceProvider = previewView.surfaceProvider
        onDispose { controller.surfaceProvider = null }
    }
}
