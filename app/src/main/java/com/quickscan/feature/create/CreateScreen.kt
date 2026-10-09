package com.quickscan.feature.create

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.R
import com.quickscan.core.qr.QrExporter
import com.quickscan.core.qr.QrLogo
import com.quickscan.core.qr.QrRenderer
import com.quickscan.core.qr.QrStyle
import com.quickscan.core.ui.component.LucideCheck
import com.quickscan.core.ui.component.LucideChevronDown
import com.quickscan.core.ui.component.LucideChevronRight
import com.quickscan.core.ui.component.LucideDownload
import com.quickscan.core.ui.component.LucideLink
import com.quickscan.core.ui.component.LucidePalette
import com.quickscan.core.ui.component.LucideType
import com.quickscan.core.ui.component.LucideUser
import com.quickscan.core.ui.component.LucideWifi
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.QSChip
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSSecondaryButton
import com.quickscan.core.ui.component.QSSwitch
import com.quickscan.core.ui.component.QSTextField
import com.quickscan.core.ui.component.QrPlaceholderView
import com.quickscan.core.ui.component.QrStyledView
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.component.QSTabBar
import com.quickscan.core.ui.component.tabBarClearance
import com.quickscan.core.ui.component.TabDestination
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space

/** Decorative art behind the preview while no payload is entered yet. */
private const val PREVIEW_PLACEHOLDER_SEED = 5_517_204L

/**
 * Exported images are always dark on white, whatever the app's theme is
 * doing: the file is going somewhere else, so it has to stand on its own.
 */
private const val EXPORT_SIZE_PX = 1024

/** Exported images sit on white whatever the app is doing. */
private fun exportStyle(style: QrStyle) = style.copy(background = Color.White.toArgb())

private val STYLE_SWATCHES = listOf(
    0xFF111318.toInt(),
    0xFF3D8FD1.toInt(),
    0xFF3F8F5B.toInt(),
    0xFFC4632B.toInt(),
    0xFF5B5BD6.toInt(),
)

private val SHAPES = listOf(
    0f to R.string.style_square,
    0.18f to R.string.style_soft,
    0.5f to R.string.style_round,
)

private val LOGO_CHOICES = listOf(
    QrLogo.None to R.string.style_none,
    QrLogo.App to R.string.style_app,
    QrLogo.Link to R.string.style_link,
    QrLogo.Wifi to R.string.style_wifi,
    QrLogo.Contact to R.string.style_contact,
)

@Composable
fun CreateScreen(
    onBack: () -> Unit,
    onTabSelected: (TabDestination) -> Unit,
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = QsTheme.palette
    val context = LocalContext.current

    // Encoding is real work, so it stays off the main thread.
    val exportBitmap by produceState<Bitmap?>(null, state.payload, state.style) {
        value = withContext(Dispatchers.Default) {
            state.payload
                .takeIf { it.isNotBlank() }
                ?.let { QrRenderer.render(it, EXPORT_SIZE_PX, exportStyle(state.style)) }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateEvent.Saved -> {
                    context.copyToClipboard(event.payload)
                    Toast.makeText(context, R.string.create_saved, Toast.LENGTH_SHORT).show()
                }

                CreateEvent.Incomplete -> Toast.makeText(
                    context,
                    R.string.create_fill_required,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(palette.bg)) {
    Column(modifier = Modifier.fillMaxSize()) {
        StatusBarSpacer()

        QSNavBar(
            title = stringResource(R.string.nav_create),
            onBack = onBack,
            actionIcon = LucideCheck,
            actionDescription = stringResource(R.string.create_save),
            onAction = viewModel::save,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.x2xl),
            verticalArrangement = Arrangement.spacedBy(Space.x2xl),
        ) {
            PreviewCard(
                payload = state.payload,
                caption = state.caption,
                style = state.style,
            )

            exportBitmap?.let { bitmap ->
                ExportRow(
                    bitmap = bitmap,
                    onSave = {
                        QrExporter.saveToGallery(context, bitmap)
                            .onSuccess {
                                Toast.makeText(
                                    context,
                                    R.string.qr_image_saved,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    R.string.qr_image_save_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                    },
                    onShare = {
                        context.startActivity(
                            QrExporter.shareIntent(
                                context = context,
                                bitmap = bitmap,
                                chooserTitle = context.getString(R.string.share_qr_chooser),
                            ),
                        )
                    },
                )
            }

            Text(
                text = stringResource(R.string.create_content_type),
                style = QsTheme.text.label11,
                color = palette.inkFaint,
            )

            TypePicker(
                selected = state.type,
                onSelect = viewModel::setType,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
                QSTextField(
                    value = state.label,
                    onValueChange = viewModel::setLabel,
                    label = stringResource(R.string.create_field_label),
                )

                when (state.type) {
                    CreateType.Link -> {
                        QSTextField(
                            value = state.url,
                            onValueChange = viewModel::setUrl,
                            label = stringResource(R.string.create_field_url),
                            placeholder = stringResource(R.string.paste_link_hint),
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        )
                        QSTextField(
                            value = state.note,
                            onValueChange = viewModel::setNote,
                            label = stringResource(R.string.create_field_note),
                            placeholder = stringResource(R.string.create_note_placeholder),
                        )
                    }

                    CreateType.Text -> QSTextField(
                        value = state.text,
                        onValueChange = viewModel::setText,
                        label = stringResource(R.string.create_field_text),
                        placeholder = stringResource(R.string.create_text_placeholder),
                        singleLine = false,
                    )

                    CreateType.Wifi -> {
                        QSTextField(
                            value = state.ssid,
                            onValueChange = viewModel::setSsid,
                            label = stringResource(R.string.create_field_ssid),
                        )
                        QSTextField(
                            value = state.password,
                            onValueChange = viewModel::setPassword,
                            label = stringResource(R.string.create_field_password),
                            isPassword = true,
                            imeAction = ImeAction.Done,
                        )
                        QSTextField(
                            value = state.security,
                            onValueChange = viewModel::setSecurity,
                            label = stringResource(R.string.create_field_security),
                            placeholder = stringResource(R.string.create_security_wpa),
                        )
                        HiddenNetworkRow(
                            hidden = state.hidden,
                            onToggle = viewModel::setHidden,
                        )
                    }

                    CreateType.Contact -> {
                        QSTextField(
                            value = state.contactName,
                            onValueChange = viewModel::setContactName,
                            label = stringResource(R.string.create_field_name),
                        )
                        QSTextField(
                            value = state.contactPhone,
                            onValueChange = viewModel::setContactPhone,
                            label = stringResource(R.string.create_field_phone),
                            keyboardType = KeyboardType.Phone,
                        )
                        QSTextField(
                            value = state.contactEmail,
                            onValueChange = viewModel::setContactEmail,
                            label = stringResource(R.string.create_field_email),
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                        )
                    }
                }
            }

            StyleRow(
                open = state.styleOpen,
                onToggle = { viewModel.setStyleOpen(!state.styleOpen) },
            )

            if (state.styleOpen) {
                StylePanel(
                    style = state.style,
                    onForeground = viewModel::setForeground,
                    onRadius = viewModel::setCornerRadius,
                    onLogo = viewModel::setLogo,
                    onReset = viewModel::resetStyle,
                )
            }

            QSPrimaryButton(
                label = stringResource(R.string.create_save),
                onClick = viewModel::save,
                enabled = state.canSave && !state.saving,
                icon = LucideCheck,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(tabBarClearance()))
        }
    }

    QSTabBar(
        selected = TabDestination.Create,
        onSelect = onTabSelected,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
    }
}

@Composable
private fun ExportRow(
    bitmap: Bitmap,
    onSave: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Text(
            text = stringResource(R.string.export_section),
            style = QsTheme.text.label11,
            color = palette.inkFaint,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            QSPrimaryButton(
                label = stringResource(R.string.save_qr_to_gallery),
                onClick = onSave,
                icon = LucideDownload,
                modifier = Modifier.weight(1f),
            )
            QSSecondaryButton(
                label = stringResource(R.string.share_qr_image),
                onClick = onShare,
                ink = palette.ink,
                container = palette.surface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PreviewCard(payload: String, caption: String, style: QrStyle) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.hero)
            .background(palette.surface)
            .padding(horizontal = Space.section, vertical = Space.x4xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        Box(
            modifier = Modifier
                .size(176.dp)
                .clip(Radius.lg)
                .background(palette.surfaceElevated)
                .padding(9.dp),
        ) {
            if (payload.isBlank()) {
                QrPlaceholderView(
                    seed = PREVIEW_PLACEHOLDER_SEED,
                    foreground = palette.hairline,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                QrStyledView(
                    payload = payload,
                    style = style.copy(background = Color.White.toArgb()),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Text(
            text = caption.ifBlank { stringResource(R.string.create_preview_placeholder) },
            style = text.chip13.copy(fontWeight = FontWeight.Medium),
            color = palette.inkMuted,
            maxLines = 2,
        )
    }
}

@Composable
private fun TypePicker(selected: CreateType, onSelect: (CreateType) -> Unit) {
    val palette = QsTheme.palette

    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        TypeOption(
            type = CreateType.Link,
            icon = LucideLink,
            selected = selected == CreateType.Link,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        TypeOption(
            type = CreateType.Text,
            icon = LucideType,
            selected = selected == CreateType.Text,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        TypeOption(
            type = CreateType.Wifi,
            icon = LucideWifi,
            selected = selected == CreateType.Wifi,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        TypeOption(
            type = CreateType.Contact,
            icon = LucideUser,
            selected = selected == CreateType.Contact,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TypeOption(
    type: CreateType,
    icon: ImageVector,
    selected: Boolean,
    onSelect: (CreateType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette

    val typeLabel = stringResource(type.labelRes)

    Column(
        modifier = modifier
            .height(74.dp)
            .clip(Radius.lg)
            .background(if (selected) palette.accent else palette.surface)
            .clickable { onSelect(type) }
            .semantics { contentDescription = typeLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) palette.accentOn else palette.ink,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = typeLabel,
            style = QsTheme.text.label11,
            color = if (selected) palette.accentOn else palette.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun HiddenNetworkRow(hidden: Boolean, onToggle: (Boolean) -> Unit) {
    val palette = QsTheme.palette

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.xl)
            .background(palette.surface)
            .padding(horizontal = Space.xl, vertical = Space.lg),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(Radius.xs)
                .background(palette.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LucideWifi,
                contentDescription = null,
                tint = palette.accentTintInk,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = stringResource(R.string.create_field_hidden),
            style = QsTheme.text.row15,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
        QSSwitch(checked = hidden, onCheckedChange = onToggle)
    }
}

@Composable
private fun StyleRow(
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.xl)
            .background(palette.surface)
            .clickable(onClick = onToggle)
            .padding(horizontal = Space.xl, vertical = Space.lg),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = LucidePalette,
            contentDescription = null,
            tint = palette.ink,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.create_style_colours),
            style = QsTheme.text.body14.copy(fontWeight = FontWeight.Medium),
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (open) LucideChevronDown else LucideChevronRight,
            contentDescription = null,
            tint = palette.inkFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Colour, shape and logo controls for the code being created. */
@Composable
private fun StylePanel(
    style: QrStyle,
    onForeground: (Int) -> Unit,
    onRadius: (Float) -> Unit,
    onLogo: (QrLogo) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.xl)
            .background(palette.surface)
            .padding(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        StyleLabel(stringResource(R.string.style_foreground))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            STYLE_SWATCHES.forEach { colour ->
                Swatch(
                    colour = colour,
                    selected = style.foreground == colour,
                    onClick = { onForeground(colour) },
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.style_reset),
                style = QsTheme.text.chip13,
                color = palette.accentTintInk,
                modifier = Modifier
                    .clip(Radius.md)
                    .clickable(onClick = onReset)
                    .padding(horizontal = Space.md, vertical = Space.xs),
            )
        }

        StyleLabel(stringResource(R.string.style_shape))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            SHAPES.forEach { (radius, label) ->
                QSChip(
                    label = stringResource(label),
                    selected = style.cornerRadius == radius,
                    onClick = { onRadius(radius) },
                )
            }
        }

        StyleLabel(stringResource(R.string.style_logo))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            LOGO_CHOICES.forEach { (logo, label) ->
                QSChip(
                    label = stringResource(label),
                    selected = style.logo == logo,
                    onClick = { onLogo(logo) },
                )
            }
        }
    }
}

@Composable
private fun StyleLabel(text: String) {
    Text(
        text = text,
        style = QsTheme.text.chip13,
        color = QsTheme.palette.inkFaint,
    )
}

@Composable
private fun Swatch(colour: Int, selected: Boolean, onClick: () -> Unit) {
    val palette = QsTheme.palette
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(colour))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) palette.accent else palette.hairline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("QuickScan", text))
}
