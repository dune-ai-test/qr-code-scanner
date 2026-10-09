package com.quickscan.feature.create

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.R
import com.quickscan.core.ui.component.LucideCheck
import com.quickscan.core.ui.component.LucideLink
import com.quickscan.core.ui.component.LucidePalette
import com.quickscan.core.ui.component.LucideType
import com.quickscan.core.ui.component.LucideUser
import com.quickscan.core.ui.component.LucideWifi
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSSwitch
import com.quickscan.core.ui.component.QSTextField
import com.quickscan.core.ui.component.QrCodeView
import com.quickscan.core.ui.component.QrPlaceholderView
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.component.QSTabBar
import com.quickscan.core.ui.component.TabDestination
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space

/** Decorative art behind the preview while no payload is entered yet. */
private const val PREVIEW_PLACEHOLDER_SEED = 5_517_204L

@Composable
fun CreateScreen(
    onBack: () -> Unit,
    onTabSelected: (TabDestination) -> Unit,
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = QsTheme.palette
    val context = LocalContext.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
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
            PreviewCard(payload = state.payload, caption = state.caption)

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

            StyleRow()

            QSPrimaryButton(
                label = stringResource(R.string.create_save),
                onClick = viewModel::save,
                enabled = state.canSave && !state.saving,
                icon = LucideCheck,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Space.x2xl))
        }

        QSTabBar(selected = TabDestination.Create, onSelect = onTabSelected)
    }
}

@Composable
private fun PreviewCard(payload: String, caption: String) {
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
                QrCodeView(
                    content = payload,
                    foreground = palette.ink,
                    background = palette.surfaceElevated,
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
private fun StyleRow() {
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
    }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("QuickScan", text))
}
