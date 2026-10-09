package com.quickscan.feature.scanner

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.TorchState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.quickscan.data.barcode.DecodedCode
import com.quickscan.core.qr.DecodeImages
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
/**
 * Analysis resolution. CameraX defaults to 640x480, which is too coarse for a
 * QR code at a normal scanning distance.
 */
private const val ANALYSIS_WIDTH = 1280
private const val ANALYSIS_HEIGHT = 720

class CameraController(
    private val context: Context,
    private val decoder: ZxingDecoder,
    private val onCode: (DecodedCode) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var provider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var preview: Preview? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preferFront: Boolean = false
    private val decoding = AtomicBoolean(false)

    @Volatile
    private var paused: Boolean = false

    @Volatile
    private var autoDetect: Boolean = true

    /**
     * With auto-detect off the analysis stream still has to run to keep the
     * preview and the reticle alive, but every frame is dropped without
     * decoding. The shutter button is the only way a code gets read.
     */
    fun setAutoDetect(enabled: Boolean) {
        autoDetect = enabled
    }

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
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .build()
            .apply { setAnalyzer(analysisExecutor, ::analyse) }
        analysis = analysisUseCase

        val captureUseCase = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = captureUseCase

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val useCases = arrayOf(previewUseCase, analysisUseCase, captureUseCase)

        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(owner, selector, *useCases)
        }.onFailure {
            // Devices with a single lens fall back to whatever camera exists.
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *useCases,
                )
            }.onSuccess { camera = it }
        }.onSuccess { camera = it }
    }

    /**
     * Grabs a single frame and runs it through the same decoder the analyser
     * uses. This is the shutter button's whole job, and it works whether
     * auto-detect is on or off.
     */
    fun capture(onResult: (DecodedCode?) -> Unit) {
        val captureUseCase = imageCapture
        if (captureUseCase == null) {
            onResult(null)
            return
        }
        // Hold the analyser off while the frame is grabbed so the two do not
        // compete for the single analysis thread.
        paused = true
        try {
            captureUseCase.takePicture(
                analysisExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val code = try {
                            image.use { DecodeImages.fromCapture(it)?.let(decoder::decode) }
                        } catch (_: Throwable) {
                            null
                        }
                        onResult(code)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        onResult(null)
                    }
                },
            )
        } catch (_: Throwable) {
            onResult(null)
        } finally {
            // The analyser resumes on the next resume() or immediately if the
            // caller is still in auto-detect mode.
            if (autoDetect) paused = false
        }
    }

    private fun analyse(image: ImageProxy) {
        if (paused || !autoDetect || !decoding.compareAndSet(false, true)) {
            image.close()
            return
        }
        try {
            decoder.decode(image)?.let(onCode)
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