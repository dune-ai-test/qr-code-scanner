package com.quickscan.feature.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.R
import com.quickscan.core.ui.labelRes
import com.quickscan.core.qr.DecodeImages
import com.quickscan.core.ui.component.LucideClipboardPaste
import com.quickscan.core.util.ClipboardGuard
import com.quickscan.core.ui.component.LucideImage
import com.quickscan.core.ui.component.LucideRepeat
import com.quickscan.core.ui.component.LucideScanLine
import com.quickscan.core.ui.component.LucideZap
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSSecondaryButton
import com.quickscan.core.ui.component.QSTabBar
import com.quickscan.core.ui.component.tabBarClearance
import com.quickscan.core.ui.component.QSTextField
import com.quickscan.core.ui.component.QrCornerBrackets
import com.quickscan.core.ui.component.QrPlaceholderView
import com.quickscan.core.ui.component.TabDestination
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.rememberReducedMotion
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.quickscan.data.barcode.ScanConfidence
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableLongStateOf
import android.os.SystemClock
import kotlinx.coroutines.withContext

private const val SAMPLE_QR_SEED = 20_240_919L

/** How long after a successful scan the "hold steady" hint stays suppressed. */
private const val HINT_HOLD_MILLIS = 1_200L


@Composable
fun ScannerScreen(
    onOpenResult: (Long) -> Unit,
    onTabSelected: (TabDestination) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = QsTheme.palette
    val text = QsTheme.text
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                uri?.let { DecodeImages.fromUri(context, it) }
            } ?: return@launch
            viewModel.onImageSelected(bitmap)
        }
    }

    val controller = remember {
        CameraController(context, viewModel.decoder) { code ->
            viewModel.onCodeDetected(code)
        }
    }

    LaunchedEffect(hasCameraPermission, state.usingFrontCamera) {
        if (hasCameraPermission) controller.bind(lifecycleOwner, state.usingFrontCamera)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.resume()
                Lifecycle.Event.ON_PAUSE -> controller.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.shutdown()
        }
    }

    val zoom by controller.zoom.collectAsStateWithLifecycle()
    val confidence by controller.confidence.collectAsStateWithLifecycle()

    // While a code is being read the hint is noise, so it is held off for a
    // moment after each accepted scan. Confidence is republished about ten
    // times a second, so the hold takes effect on the very next frame.
    var hintHoldUntil by remember { mutableLongStateOf(0L) }
    val hintVisible = state.autoDetect &&
        SystemClock.elapsedRealtime() >= hintHoldUntil
    val toneGenerator = remember {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME)
        }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { toneGenerator?.release() } }
    }

    LaunchedEffect(state.autoDetect) { controller.setAutoDetect(state.autoDetect) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScannerEvent.OpenResult -> onOpenResult(event.scanId)
                is ScannerEvent.SavedInPlace -> context.showSavedToast(event)
                is ScannerEvent.CopyToClipboard -> context.copyToClipboard(event.text)
                is ScannerEvent.Message -> context.showToast(event.text)
                is ScannerEvent.SignalScanFeedback -> {
                    hintHoldUntil = SystemClock.elapsedRealtime() + HINT_HOLD_MILLIS
                    if (event.vibrate) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (event.playSound) {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(palette.scene)) {
        Viewfinder(
            hasPermission = hasCameraPermission,
            controller = controller,
            torchOn = state.torchOn,
            autoDetect = state.autoDetect,
            continuousMode = state.continuousMode,
            confidence = confidence,
            hintVisible = hintVisible,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = context::openAppSettings,
            onTorch = {
                controller.toggleTorch().also(viewModel::onTorchChanged)
            },
            onFlip = {
                val next = !state.usingFrontCamera
                controller.switchCamera()
                viewModel.onCameraSwitched(next)
            },
            onGallery = { imagePicker.launch(pickImageRequest()) },
            onShutter = {
                if (!state.busy) {
                    viewModel.onCaptureStarted()
                    controller.capture { code -> viewModel.onCaptured(code) }
                }
            },
            onPaste = { viewModel.setPasteOpen(!state.pasteOpen) },
            zoom = zoom,
            onStepZoom = { controller.stepZoom() },
            modifier = Modifier.fillMaxSize(),
        )

        // Pasting used to be a tile under the viewfinder. With the camera
        // full-bleed it is a sheet over it, so the action survives.
        if (state.pasteOpen) {
            PasteLinkCard(
                value = state.pasteText,
                canSubmit = state.canSubmitPaste,
                onValueChange = viewModel::onPasteChanged,
                onSubmit = viewModel::submitPaste,
                onDismiss = { viewModel.setPasteOpen(false) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = Space.x2xl,
                        end = Space.x2xl,
                        bottom = tabBarClearance() + Space.xxl,
                    ),
            )
        }

        QSTabBar(
            selected = TabDestination.Scan,
            onSelect = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun pickImageRequest() =
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

/**
 * The camera scene. With permission granted this is the live preview behind the
 * reticle; without it the same dark scene hosts a sample code and the prompt.
 */
@Composable
private fun Viewfinder(
    hasPermission: Boolean,
    controller: CameraController,
    torchOn: Boolean,
    autoDetect: Boolean,
    continuousMode: Boolean,
    confidence: ScanConfidence,
    hintVisible: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onTorch: () -> Unit,
    onFlip: () -> Unit,
    onGallery: () -> Unit,
    onShutter: () -> Unit,
    onPaste: () -> Unit,
    zoom: ZoomState,
    onStepZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    // The camera now runs under the status bar, so the pills that sit over it
    // inset themselves rather than relying on a spacer above the viewfinder.
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            // The camera surface bleeds past whatever box it is given, so it
            // has to be clipped or it paints over the chrome above it.
            .clipToBounds()
            .background(palette.scene),
    ) {
        if (hasPermission) {
            CameraPreview(
                controller = controller,
                onZoom = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        // The accent glow the mockups place behind the subject.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
                .width(530.dp)
                .height(290.dp)
                .graphicsLayer { alpha = 0.55f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent, Color.Transparent),
                        radius = 265f,
                    ),
                    shape = CircleShape,
                ),
        )

        if (hasPermission) {
            Reticle(
                color = palette.accent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(274.dp),
            )
        }

        AutoDetectPill(
            enabled = autoDetect && hasPermission,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = inset + 20.dp),
        )

        // Without this, not navigating looks like the app hanging. It sits
        // under the auto-detect pill because both describe the current mode.
        if (continuousMode && hasPermission) {
            ContinuousPill(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = inset + 58.dp),
            )
        }

        ConfidenceHint(
            confidence = confidence,
            visible = hintVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = tabBarClearance() + 124.dp),
        )

        if (hasPermission) {
            ViewfinderPill(
                onClick = onTorch,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = inset + 20.dp, end = 20.dp),
            ) {
                Icon(
                    imageVector = LucideZap,
                    contentDescription = stringResource(R.string.toggle_torch),
                    tint = if (torchOn) palette.accent else Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            if (zoom.isAvailable) {
                ZoomPill(
                    ratio = zoom.current,
                    onClick = onStepZoom,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = inset + 60.dp, end = 20.dp),
                )
            }

            ViewfinderPill(
                onClick = onPaste,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = inset + if (zoom.isAvailable) 100.dp else 60.dp,
                        end = 20.dp,
                    ),
            ) {
                Icon(
                    imageVector = LucideClipboardPaste,
                    contentDescription = stringResource(R.string.paste_link),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (!hasPermission) {
            PermissionPrompt(
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Space.section),
            )
            // The shutter row belongs to the live preview, not the empty state.
        } else {
            ShutterControls(
                onGallery = onGallery,
                onShutter = onShutter,
                onFlip = onFlip,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = tabBarClearance() + Space.lg),
            )
        }
    }
}

/** Corner brackets plus the accent hairline that sweeps between them. */
@Composable
private fun Reticle(color: Color, modifier: Modifier = Modifier) {
    // With animations switched off in system settings the line sits still
    // rather than sweeping forever.
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "sweep")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sweepProgress",
    )
    val offsetFraction = if (reducedMotion) 0.5f else progress

    BoxWithConstraints(modifier = modifier) {
        // Travel is a fraction of the reticle, not of the 2dp line itself.
        val travel = maxHeight * 0.80f
        val start = maxHeight * 0.10f

        QrCornerBrackets(
            color = color,
            armLength = 38.dp,
            strokeWidth = 4.dp,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset(y = start + travel * offsetFraction)
                .shadow(7.dp, CircleShape, clip = false)
                .background(color, CircleShape),
        )
    }
}

/**
 * Says the code is nearly readable.
 *
 * Without this, a scan that is not working looks exactly like a scan of
 * nothing: the viewfinder sits there, silent, whether the camera has found a
 * code and cannot quite read it or has found nothing at all. The first is
 * worth three words of feedback and the second is not, so the finder-pattern
 * count decides which it is.
 */
@Composable
private fun ConfidenceHint(
    confidence: ScanConfidence,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val shown = visible && confidence != ScanConfidence.Nothing
    // Confidence flips between frames, so without a fade the hint strobes.
    val fade by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else 180),
        label = "hintFade",
    )
    if (fade <= 0.01f) return

    Row(
        modifier = modifier
            .graphicsLayer { alpha = fade }
            .clip(Radius.pill)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = Space.lg, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = LucideScanLine,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(
                if (confidence == ScanConfidence.Framed) {
                    R.string.hold_steady_aligned
                } else {
                    R.string.hold_steady
                },
            ),
            style = QsTheme.text.rowSub12.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

/**
 * Shows whether scanning is actually running. Green means codes are being
 * read automatically; red means the shutter button is the only way in, which
 * is the case when auto-detect is switched off in Settings.
 */
/**
 * Discrete zoom stepper, mirroring the phone-camera idiom. Pinch is the
 * continuous control; this makes the feature discoverable rather than hidden
 * in a gesture.
 */
/**
 * The chrome sitting over the viewfinder. Torch, zoom and paste are the same
 * box by construction rather than by two numbers that happen to agree.
 */
@Composable
private fun ViewfinderPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(PILL_WIDTH, PILL_HEIGHT)
            .clip(Radius.lg)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val PILL_WIDTH = 56.dp
private val PILL_HEIGHT = 32.dp

@Composable
private fun ZoomPill(ratio: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = QsTheme.palette
    val zoomDescription = stringResource(R.string.zoom_control, Zoom.label(ratio))

    ViewfinderPill(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = zoomDescription },
    ) {
        Text(
            text = Zoom.label(ratio),
            style = QsTheme.text.rowSub12.copy(fontWeight = FontWeight.SemiBold),
            color = if (ratio > 1.01f) palette.accent else Color.White,
        )
    }
}

@Composable
private fun AutoDetectPill(enabled: Boolean, modifier: Modifier = Modifier) {
    val palette = QsTheme.palette

    Row(
        modifier = modifier
            .clip(Radius.pill)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 13.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (enabled) palette.live else palette.danger),
        )
        Text(
            text = stringResource(R.string.auto_detect),
            style = QsTheme.text.rowSub12.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
        )
    }
}

/**
 * Marks continuous mode while it is on. The accent fill sets it apart from the
 * translucent auto-detect pill, which reports a capability rather than a
 * choice the user made.
 */
@Composable
private fun ContinuousPill(modifier: Modifier = Modifier) {
    val palette = QsTheme.palette
    Row(
        modifier = modifier
            .clip(Radius.pill)
            .background(palette.accent)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = LucideRepeat,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = stringResource(R.string.continuous_mode),
            style = QsTheme.text.rowSub12.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

@Composable
private fun ShutterControls(
    onGallery: () -> Unit,
    onShutter: () -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val shutterLabel = stringResource(R.string.shutter)

    Row(
        modifier = modifier.height(92.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onGallery),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LucideImage,
                contentDescription = stringResource(R.string.scan_image),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(82.dp)
                .clickable(onClick = onShutter)
                .semantics { contentDescription = shutterLabel },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .shadow(12.dp, CircleShape, spotColor = palette.accent)
                    .clip(CircleShape)
                    .background(palette.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = LucideScanLine,
                    contentDescription = stringResource(R.string.shutter),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onFlip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LucideRepeat,
                contentDescription = stringResource(R.string.toggle_camera),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PermissionPrompt(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = modifier
            .clip(Radius.hero)
            .background(Color.White.copy(alpha = 0.06f))
            .padding(Space.section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        QrPlaceholderView(
            seed = SAMPLE_QR_SEED,
            foreground = Color.White,
            modifier = Modifier.size(112.dp),
        )
        Text(
            text = stringResource(R.string.camera_permission_title),
            style = text.nav16,
            color = Color.White,
        )
        Text(
            text = stringResource(R.string.camera_permission_body),
            style = text.chip13,
            color = Color.White.copy(alpha = 0.7f),
        )
        QSPrimaryButton(
            label = stringResource(R.string.camera_permission_action),
            onClick = onRequestPermission,
            container = palette.ink,
            content = palette.bg,
            modifier = Modifier.fillMaxWidth(),
        )
        QSSecondaryButton(
            label = stringResource(R.string.camera_permission_settings),
            onClick = onOpenSettings,
            ink = Color.White,
            container = Color.White.copy(alpha = 0.14f),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PasteLinkCard(
    value: String,
    canSubmit: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.hero)
            .background(palette.surface)
            .padding(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Text(
            text = stringResource(R.string.paste_link_title),
            style = QsTheme.text.row15,
            color = palette.ink,
        )
        QSTextField(
            value = value,
            onValueChange = onValueChange,
            label = stringResource(R.string.create_field_url),
            placeholder = stringResource(R.string.paste_link_hint),
            height = 56.dp,
            radius = Radius.lg,
            background = palette.surfaceElevated,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            QSPrimaryButton(
                label = stringResource(R.string.action_done),
                onClick = onSubmit,
                enabled = canSubmit,
                disabledContainer = palette.surfaceElevated,
                modifier = Modifier.weight(1f),
            )
            QSSecondaryButton(
                label = stringResource(R.string.setting_cancel),
                onClick = onDismiss,
                ink = palette.ink,
                container = palette.surfaceElevated,
                modifier = Modifier.width(110.dp),
            )
        }
    }
}

/** Item index of the paste card: viewfinder, headline, tiles, paste. */

private const val TONE_VOLUME = 80

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.copyToClipboard(text: String) {
    ClipboardGuard.copy(this, getString(R.string.app_name), text)
}

/**
 * Confirms a continuous-mode save without leaving the viewfinder. Named rather
 * than a length, because at a table of codes "which one was that" is the whole
 * question the toast exists to answer.
 */
private fun Context.showSavedToast(event: ScannerEvent.SavedInPlace) {
    val type = getString(event.type.labelRes)
    val text = if (event.label.isBlank()) {
        getString(R.string.scanned_saved, type)
    } else {
        getString(R.string.scanned_saved_named, type, event.label)
    }
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

private fun Context.showToast(message: ScannerMessage) {
    val text = when (message) {
        ScannerMessage.NoCodeInImage -> getString(R.string.no_image_selected)
        ScannerMessage.NothingToDecode -> getString(R.string.create_fill_required)
        ScannerMessage.NotAValidLink -> getString(R.string.paste_link_hint)
        ScannerMessage.NothingInViewfinder -> getString(R.string.no_code_in_viewfinder)
    }
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}