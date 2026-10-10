package com.quickscan.feature.scanner

import android.content.Context
import android.os.SystemClock
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
import com.quickscan.data.barcode.FrameGate
import com.quickscan.data.barcode.FinderPatternScan
import com.quickscan.data.barcode.ScanConfidence
import com.quickscan.data.barcode.ZxingDecoder
import java.nio.ByteBuffer
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** What the viewfinder shows about zoom, and what a pinch starts from. */
@Immutable
data class ZoomState(
    val min: Float = 1f,
    val max: Float = 1f,
    val current: Float = 1f,
) {
    /** Nothing to zoom into when the lens is fixed focal. */
    val isAvailable: Boolean get() = max > min * 1.05f
}

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

    /**
     * Cuts the per-frame cost. Decoding every frame of a 720p stream is most
     * of a megapixel of work thirty times a second for a viewfinder that is
     * usually still.
     */
    private val frameGate = FrameGate()

    /**
     * How close the last analysed frame is to reading. Published so the
     * viewfinder can say something while a decode keeps failing, which is
     * otherwise indistinguishable from pointing at nothing.
     */
    private val _confidence = MutableStateFlow(ScanConfidence.Nothing)
    val confidence: StateFlow<ScanConfidence> = _confidence.asStateFlow()

    @Volatile
    private var paused: Boolean = false

    @Volatile
    private var autoDetect: Boolean = true

    /** Zoom the lens supports, published once the camera is bound. */
    private val _zoom = MutableStateFlow(ZoomState())
    val zoom: StateFlow<ZoomState> = _zoom.asStateFlow()

    /**
     * Applies a zoom ratio, clamped to what this lens supports. Returns the
     * ratio actually applied, which may differ if the request was out of range.
     */
    fun setZoomRatio(ratio: Float): Float {
        val camera = camera
        val bounds = _zoom.value
        val clamped = Zoom.clamp(ratio, bounds.min, bounds.max)
        if (camera == null) {
            _zoom.value = bounds.copy(current = clamped)
            return clamped
        }
        camera.cameraControl.setZoomRatio(clamped)
        // The control is async; reflect the intent immediately so the UI and
        // a pinch do not feel like they are fighting the lens.
        _zoom.value = bounds.copy(current = clamped)
        return clamped
    }

    /**
     * Steps through the detents a phone camera actually offers, which is what
     * a tap on the zoom pill should do. Anything the lens cannot reach is
     * skipped rather than silently clamped.
     */
    fun stepZoom(): Float {
        val bounds = _zoom.value
        return setZoomRatio(Zoom.nextStep(bounds.current, bounds.min, bounds.max))
    }

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
        }.onSuccess { bound ->
            camera = bound
            publishZoomBounds(bound)
        }
    }

    /**
     * The zoom range is a property of the bound camera, and `zoomState.value`
     * is null until the camera has reported one. Until it does, the default
     * 1x-to-1x range reads as "this lens cannot zoom", which is the honest
     * answer for as long as it is true.
     */
    private fun publishZoomBounds(camera: Camera) {
        val state = camera.zoomState.value ?: return
        _zoom.value = ZoomState(
            min = state.minZoomRatio,
            max = state.maxZoomRatio,
            current = state.zoomRatio,
        )
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
            val now = SystemClock.elapsedRealtime()
            val plane = image.planes[0]
            val sample = peekSample(plane.buffer, plane.rowStride, width = image.width)

            if (!frameGate.shouldDecode(sample.data, sample.offset, sample.step, now)) {
                return
            }

            // Read the confidence before decoding: it explains a failure, and
            // by the time the decode has failed the caller has already shown
            // nothing at all.
            publishConfidence(image)

            val code = decoder.decode(image)
            if (code != null) {
                frameGate.onDecoded()
                onCode(code)
                return
            }

            // The viewfinder is busy but the low-resolution stream has read
            // nothing: spend one full-resolution still on it, which is what
            // finds a small or distant code.
            if (frameGate.shouldCaptureFullRes(now)) {
                escalateToFullResolution()
            }
        } finally {
            decoding.set(false)
            image.close()
        }
    }

    /**
     * Reads a few scanlines out of the luma plane and reports how close the
     * frame is to reading.
     *
     * The geometry here is deliberately identical to [ZxingDecoder]'s: same
     * width, height and row stride, same origin at zero. A confidence
     * measured against different pixels from the ones the decoder sees would
     * be describing a different frame, and the two would disagree exactly when
     * it matters.
     *
     * A camera whose plane cannot be indexed is not a reason to take the
     * scanner down, so anything unexpected leaves the hint off rather than
     * propagating out of the analysis thread.
     */
    private fun publishConfidence(image: ImageProxy) {
        val confidence = runCatching { readConfidence(image) }
            .getOrDefault(ScanConfidence.Nothing)
        _confidence.value = confidence
    }

    private fun readConfidence(image: ImageProxy): ScanConfidence {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        if (width <= 0 || height <= 0 || rowStride <= 0) return ScanConfidence.Nothing

        val step = (height / FinderPatternScan.LINE_COUNT).coerceAtLeast(1)
        val rows = ArrayList<ByteArray>(FinderPatternScan.LINE_COUNT)
        for (line in 0 until FinderPatternScan.LINE_COUNT) {
            val y = line * step
            if (y >= height) break
            // Absolute reads, so the buffer's position never matters and no
            // row has to be copied in full.
            if (y * rowStride + width > buffer.limit()) return ScanConfidence.Nothing
            val row = ByteArray(width)
            var index = y * rowStride
            for (x in 0 until width) {
                row[x] = buffer.get(index + x)
            }
            rows += row
        }
        return FinderPatternScan.scan(rows, width)
    }

    /** A cheap signature over a sparse slice of the plane, without copying it. */
    private fun peekSample(
        buffer: ByteBuffer,
        rowStride: Int,
        width: Int,
    ): Sample {
        val step = maxOf(1, width / 16)
        val cap = 4096
        val out = ByteArray(minOf(cap, maxOf(1, buffer.remaining() / step)))
        val duplicate = buffer.duplicate()
        var read = 0
        var index = 0
        while (read < out.size && index < buffer.limit()) {
            out[index++] = duplicate.get(index)
            index += step
        }
        return Sample(out, 0, step)
    }

    private class Sample(val data: ByteArray, val offset: Int, val step: Int)

    private fun escalateToFullResolution() {
        val captureUseCase = imageCapture ?: return
        // The live analyser must not fight the still for the executor.
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
                        if (code != null) {
                            frameGate.onDecoded()
                            onCode(code)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) = Unit
                },
            )
        } catch (_: Throwable) {
            // Nothing to do; the live path keeps trying.
        } finally {
            if (autoDetect) paused = false
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
        // A different lens has its own range and usually starts wide.
        _zoom.value = ZoomState()
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