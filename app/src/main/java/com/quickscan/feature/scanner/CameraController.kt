package com.quickscan.feature.scanner

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.TorchState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.quickscan.data.barcode.DecodedCode
import com.quickscan.data.barcode.ZxingDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Owns the CameraX pipeline: a preview plus an analysis stream that feeds every
 * frame to [ZxingDecoder]. Frames are analysed one at a time and dropped while
 * one is still in flight, which keeps the preview at full frame rate.
 */
class CameraController(
    private val context: Context,
    private val decoder: ZxingDecoder,
    private val onCode: (DecodedCode) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var provider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var preview: Preview? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preferFront: Boolean = false
    private val decoding = AtomicBoolean(false)

    @Volatile
    private var paused: Boolean = false

    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Supplied by the preview composable; binding waits for it. */
    var surfaceProvider: Preview.SurfaceProvider? = null
        set(value) {
            field = value
            preview?.setSurfaceProvider(value)
            if (value != null) bindUseCases()
        }

    suspend fun bind(owner: LifecycleOwner, preferFrontCamera: Boolean) {
        lifecycleOwner = owner
        preferFront = preferFrontCamera
        lensFacing = if (preferFrontCamera) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        provider = obtainProvider()
        bindUseCases()
    }

    private fun bindUseCases() {
        val cameraProvider = provider ?: return
        val owner = lifecycleOwner ?: return
        val surfaceProvider = surfaceProvider ?: return

        val previewUseCase = Preview.Builder().build().apply {
            setSurfaceProvider(surfaceProvider)
        }
        preview = previewUseCase

        val analysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .apply { setAnalyzer(analysisExecutor, ::analyse) }
        analysis = analysisUseCase

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(owner, selector, previewUseCase, analysisUseCase)
        }.onFailure {
            // Devices with a single lens fall back to whatever camera exists.
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    analysisUseCase,
                )
            }.onSuccess {
                camera = it
            }
        }.onSuccess {
            camera = it
        }
    }

    private fun analyse(image: androidx.camera.core.ImageProxy) {
        if (paused || !decoding.compareAndSet(false, true)) {
            image.close()
            return
        }
        try {
            val code = decoder.decode(image)
            android.util.Log.i(
                "QsDecode",
                "frame ${image.width}x${image.height} rot=${image.imageInfo.rotationDegrees} " +
                    "result=$code",
            )
            code?.let(onCode)
        } catch (t: Throwable) {
            android.util.Log.e("QsDecode", "frame failed", t)
        } finally {
            decoding.set(false)
            image.close()
        }
    }

    fun toggleTorch(): Boolean {
        val camera = camera ?: return false
        val next = camera.cameraInfo.torchState.value != TorchState.ON
        camera.cameraControl.enableTorch(next)
        return next
    }

    fun switchCamera() {
        preferFront = lensFacing == CameraSelector.LENS_FACING_BACK
        lensFacing = if (preferFront) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindUseCases()
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun shutdown() {
        runCatching { analysisExecutor.shutdown() }
        runCatching { provider?.unbindAll() }
        scope.cancel()
    }

    private suspend fun obtainProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    runCatching { future.get() }
                        .onSuccess { continuation.resume(it) }
                        .onFailure { continuation.resumeWithException(it) }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
}