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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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
import com.quickscan.core.ui.ScanDates
import com.quickscan.core.ui.icon
import com.quickscan.core.ui.labelRes
import com.quickscan.core.ui.payloadTileColors
import com.quickscan.core.qr.DecodeImages
import com.quickscan.core.ui.component.LucideClipboardPaste
import com.quickscan.core.util.ClipboardGuard
import com.quickscan.core.ui.component.LucideImage
import com.quickscan.core.ui.component.LucideRepeat
import com.quickscan.core.ui.component.LucideScanLine
import com.quickscan.core.ui.component.LucideZap
import com.quickscan.core.ui.component.QSActionTile
import com.quickscan.core.ui.component.QSListRow
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSSecondaryButton
import com.quickscan.core.ui.component.QSTabBar
import com.quickscan.core.ui.component.tabBarClearance
import com.quickscan.core.ui.component.QSTextField
import com.quickscan.core.ui.component.QrCornerBrackets
import com.quickscan.core.ui.component.QrPlaceholderView
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.component.TabDestination
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.rememberReducedMotion
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space
import com.quickscan.data.barcode.PayloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SAMPLE_QR_SEED = 20_240_919L


@Composable
fun ScannerScreen(
    onOpenResult: (Long) -> Unit,
    onOpenHistory: () -> Unit,
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
    val listState = rememberLazyListState()

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
    val toneGenerator = remember {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME)
        }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { toneGenerator?.release() } }
    }

    // The paste card is inserted below the fold, so bring it into view.
    LaunchedEffect(state.pasteOpen) {
        if (state.pasteOpen) listState.animateScrollToItem(PASTE_CARD_ITEM)
    }

    LaunchedEffect(state.autoDetect) { controller.setAutoDetect(state.autoDetect) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScannerEvent.OpenResult -> onOpenResult(event.scanId)
                is ScannerEvent.CopyToClipboard -> context.copyToClipboard(event.text)
                is ScannerEvent.Message -> context.showToast(event.text)
                is ScannerEvent.SignalScanFeedback -> {
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

    Box(modifier = Modifier.fillMaxSize().background(palette.bg)) {
    Column(modifier = Modifier.fillMaxSize()) {
        StatusBarSpacer()

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Space.x2xl),
        ) {
            item {
                Viewfinder(
                    hasPermission = hasCameraPermission,
                    controller = controller,
                    torchOn = state.torchOn,
                    autoDetect = state.autoDetect,
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
                    zoom = zoom,
                    onStepZoom = { controller.stepZoom() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = Space.x2xl),
                    verticalArrangement = Arrangement.spacedBy(Space.lg),
                ) {
                    Text(
                        text = stringResource(R.string.scanner_headline),
                        style = text.title26,
                        color = palette.ink,
                    )
                    Text(
                        text = stringResource(R.string.scanner_subhead),
                        style = text.body14,
                        color = palette.inkMuted,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = Space.x2xl),
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    QSActionTile(
                        icon = LucideImage,
                        label = stringResource(R.string.scan_image),
                        onClick = { imagePicker.launch(pickImageRequest()) },
                        modifier = Modifier.weight(1f),
                    )
                    QSActionTile(
                        icon = LucideClipboardPaste,
                        label = stringResource(R.string.paste_link),
                        onClick = { viewModel.setPasteOpen(true) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.pasteOpen) {
                item {
                    PasteLinkCard(
                        value = state.pasteText,
                        canSubmit = state.canSubmitPaste,
                        onValueChange = viewModel::onPasteChanged,
                        onSubmit = viewModel::submitPaste,
                        onDismiss = { viewModel.setPasteOpen(false) },
                        modifier = Modifier.padding(horizontal = Space.x2xl),
                    )
                }
            }

            if (state.recent.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.x2xl),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.recent_scans),
                            style = text.label11,
                            color = palette.inkFaint,
                        )
                        Text(
                            text = stringResource(R.string.see_all),
                            style = text.label11,
                            color = palette.accentTintInk,
                            modifier = Modifier.clickable(onClick = onOpenHistory),
                        )
                    }
                }

                items(state.recent, key = { it.id }) { scan ->
                    val type = runCatching { PayloadType.valueOf(scan.type) }
                        .getOrDefault(PayloadType.Text)
                    val (tileBackground, tileInk) = payloadTileColors(type)

                    QSListRow(
                        icon = type.icon,
                        iconTint = tileInk,
                        iconBackground = tileBackground,
                        title = scan.title,
                        subtitle = "${stringResource(type.labelRes)} · " +
                            ScanDates.relative(scan.createdAt),
                        background = palette.bg,
                        modifier = Modifier.padding(horizontal = Space.x2xl),
                        onClick = { onOpenResult(scan.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(tabBarClearance())) }
        }
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
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onTorch: () -> Unit,
    onFlip: () -> Unit,
    onGallery: () -> Unit,
    onShutter: () -> Unit,
    zoom: ZoomState,
    onStepZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(466.dp)
            // The camera surface would otherwise bleed past the viewfinder and
            // paint over the headline below it.
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
                .padding(20.dp),
        )

        if (hasPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp)
                    .size(56.dp, 32.dp)
                    .clip(Radius.lg)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onTorch),
                contentAlignment = Alignment.Center,
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
                        .padding(top = 60.dp, end = 20.dp),
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
                modifier = Modifier.align(Alignment.BottomCenter),
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
 * Shows whether scanning is actually running. Green means codes are being
 * read automatically; red means the shutter button is the only way in, which
 * is the case when auto-detect is switched off in Settings.
 */
/**
 * Discrete zoom stepper, mirroring the phone-camera idiom. Pinch is the
 * continuous control; this makes the feature discoverable rather than hidden
 * in a gesture.
 */
@Composable
private fun ZoomPill(ratio: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = QsTheme.palette

    Box(
        modifier = modifier
            .clip(Radius.lg)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = stringResource(R.string.zoom_control, Zoom.label(ratio))
            }
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
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
private const val PASTE_CARD_ITEM = 3

private const val TONE_VOLUME = 80

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.copyToClipboard(text: String) {
    ClipboardGuard.copy(this, getString(R.string.app_name), text)
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