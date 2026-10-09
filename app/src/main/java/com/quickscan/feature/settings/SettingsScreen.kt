package com.quickscan.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.BuildConfig
import com.quickscan.R
import com.quickscan.core.ui.theme.Accent
import com.quickscan.core.ui.theme.Metrics
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.swatch
import com.quickscan.core.ui.theme.swatchOn
import com.quickscan.core.ui.theme.Space
import com.quickscan.core.ui.component.LucideBell
import com.quickscan.core.ui.component.LucideBookmark
import com.quickscan.core.ui.component.LucideCamera
import com.quickscan.core.ui.component.LucideChevronRight
import com.quickscan.core.ui.component.LucideClipboardCheck
import com.quickscan.core.ui.component.LucideGlobe
import com.quickscan.core.ui.component.LucideHardDrive
import com.quickscan.core.ui.component.LucideImage
import com.quickscan.core.ui.component.LucideInfo
import com.quickscan.core.ui.component.LucideMoon
import com.quickscan.core.ui.component.LucideScanLine
import com.quickscan.core.ui.component.LucideSparkles
import com.quickscan.core.ui.component.LucideSmartphone
import com.quickscan.core.ui.component.LucideTrash2
import com.quickscan.core.ui.component.LucideType
import com.quickscan.core.ui.component.LucideUser
import com.quickscan.core.ui.component.LucideVideo
import com.quickscan.core.ui.component.LucideVolume2
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.QSSectionHeader
import com.quickscan.core.ui.component.QSSettingRow
import com.quickscan.core.ui.component.QSSwitch
import com.quickscan.core.ui.component.QSTabBar
import com.quickscan.core.ui.component.QSValueSlot
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.component.TabDestination
import com.quickscan.data.repository.StorageUsage

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onTabSelected: (TabDestination) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val palette = QsTheme.palette

    var confirmClear by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshStorage() }
    val imagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshStorage() }
    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshStorage() }

    var cameraGranted by remember { mutableStateOf(context.isGranted(Manifest.permission.CAMERA)) }
    var imagesGranted by remember {
        mutableStateOf(context.isGranted(Manifest.permission.READ_MEDIA_IMAGES))
    }
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.isGranted(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        StatusBarSpacer()

        QSNavBar(
            title = stringResource(R.string.nav_settings),
            onBack = onBack,
            actionIcon = LucideUser,
            actionDescription = stringResource(R.string.settings_profile_placeholder),
            onAction = {},
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            ProfileCard(name = state.displayName)

            QSSectionHeader(text = stringResource(R.string.settings_section_scanner))
            SettingsGroup {
                QSSettingRow(
                    icon = LucideScanLine,
                    title = stringResource(R.string.setting_auto_detect),
                    background = palette.surface,
                    trailing = {
                        QSSwitch(state.autoDetect, viewModel::setAutoDetect)
                    },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideVideo,
                    title = stringResource(R.string.setting_default_camera),
                    background = palette.surface,
                    onClick = { viewModel.setPreferFrontCamera(!state.preferFrontCamera) },
                    trailing = {
                        QSValueSlot(
                            value = stringResource(
                                if (state.preferFrontCamera) {
                                    R.string.setting_camera_front
                                } else {
                                    R.string.setting_camera_rear
                                },
                            ),
                        )
                    },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideClipboardCheck,
                    title = stringResource(R.string.setting_copy_automatically),
                    background = palette.surface,
                    trailing = {
                        QSSwitch(state.copyAutomatically, viewModel::setCopyAutomatically)
                    },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideVolume2,
                    title = stringResource(R.string.setting_scan_sound),
                    background = palette.surface,
                    trailing = { QSSwitch(state.scanSound, viewModel::setScanSound) },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideSmartphone,
                    title = stringResource(R.string.setting_vibrate),
                    background = palette.surface,
                    trailing = { QSSwitch(state.vibrate, viewModel::setVibrate) },
                )
            }

            QSSectionHeader(text = stringResource(R.string.settings_section_appearance))
            SettingsGroup {
                QSSettingRow(
                    icon = LucideMoon,
                    title = stringResource(R.string.setting_dark_mode),
                    background = palette.surface,
                    trailing = { QSSwitch(state.theme.darkMode, viewModel::setDarkMode) },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideGlobe,
                    title = stringResource(R.string.setting_accent),
                    background = palette.surface,
                    trailing = {
                        AccentSwitcher(
                            current = state.theme.accent,
                            onSelect = viewModel::setAccent,
                        )
                    },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideType,
                    title = stringResource(R.string.setting_larger_text),
                    background = palette.surface,
                    trailing = { QSSwitch(state.theme.largerText, viewModel::setLargerText) },
                )
            }

            QSSectionHeader(text = stringResource(R.string.settings_section_storage))
            SettingsGroup {
                QSSettingRow(
                    icon = LucideBookmark,
                    title = if (state.retentionDays > 0) {
                        stringResource(R.string.setting_retention, state.retentionDays)
                    } else {
                        stringResource(R.string.setting_retention_forever)
                    },
                    background = palette.surface,
                    onClick = {
                        viewModel.setRetentionDays(
                            if (state.retentionDays >= 90) 7 else state.retentionDays + 30,
                        )
                    },
                    trailing = {
                        QSValueSlot(value = retentionLabel(state.retentionDays))
                    },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideHardDrive,
                    title = stringResource(R.string.setting_storage_used),
                    background = palette.surface,
                    trailing = { QSValueSlot(value = state.storage.readableSize()) },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideTrash2,
                    title = stringResource(R.string.setting_clear_history),
                    background = palette.surface,
                    onClick = { confirmClear = true },
                )
            }

            QSSectionHeader(text = stringResource(R.string.settings_section_permissions))
            SettingsGroup {
                QSSettingRow(
                    icon = LucideCamera,
                    title = stringResource(R.string.permission_camera),
                    background = palette.surface,
                    onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    trailing = {
                        QSValueSlot(
                            value = stringResource(
                                if (cameraGranted) R.string.permission_allowed
                                else R.string.permission_denied,
                            ),
                        )
                    },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideImage,
                    title = stringResource(R.string.permission_photo_library),
                    background = palette.surface,
                    onClick = {
                        if (imagesGranted) {
                            context.openAppSettings()
                        } else {
                            imagesLauncher.launch(readMediaImagesPermission())
                        }
                    },
                    trailing = {
                        QSValueSlot(
                            value = stringResource(
                                if (imagesGranted) R.string.permission_allowed
                                else R.string.permission_denied,
                            ),
                        )
                    },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideBell,
                    title = stringResource(R.string.permission_notifications),
                    background = palette.surface,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !notificationsGranted
                        ) {
                            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.openAppSettings()
                        }
                    },
                    trailing = {
                        QSValueSlot(
                            value = when {
                                !notificationsGranted &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                                    stringResource(R.string.permission_off)

                                else -> stringResource(R.string.permission_allowed)
                            },
                        )
                    },
                )
            }

            QSSectionHeader(text = stringResource(R.string.settings_section_about))
            SettingsGroup {
                QSSettingRow(
                    icon = LucideInfo,
                    title = stringResource(R.string.setting_version),
                    background = palette.surface,
                    trailing = { QSValueSlot(value = BuildConfig.VERSION_NAME) },
                )
                GroupDivider()
                QSSettingRow(
                    icon = LucideSparkles,
                    title = stringResource(R.string.setting_whats_new),
                    background = palette.surface,
                    onClick = {},
                    trailing = {
                        Icon(
                            imageVector = LucideChevronRight,
                            contentDescription = null,
                            tint = palette.inkFaint,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            Spacer(Modifier.height(Space.xl))
        }

        QSTabBar(selected = TabDestination.Settings, onSelect = onTabSelected)
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.setting_clear_history_confirm_title)) },
            text = { Text(stringResource(R.string.setting_clear_history_confirm_body)) },
            confirmButton = {
                Text(
                    text = stringResource(R.string.setting_clear_history_confirm),
                    color = palette.danger,
                    modifier = Modifier.clickable {
                        viewModel.clearHistory()
                        confirmClear = false
                    },
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(R.string.setting_cancel),
                    modifier = Modifier.clickable { confirmClear = false },
                )
            },
            containerColor = palette.surface,
        )
    }
}

@Composable
private fun ProfileCard(name: String) {
    val palette = QsTheme.palette
    val displayName = name.ifBlank { stringResource(R.string.settings_profile_placeholder) }
    val initials = displayName.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "·" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x2xl)
            .clip(Radius.xxl)
            .background(palette.surface)
            .padding(Space.xl),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Metrics.profileAvatar)
                .clip(CircleShape)
                .background(palette.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = QsTheme.text.title18,
                color = palette.accentTintInk,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, style = QsTheme.text.nav16, color = palette.ink)
        }
        Icon(
            imageVector = LucideChevronRight,
            contentDescription = null,
            tint = palette.inkFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** The four accent swatches, mirroring the picker the mockups describe. */
@Composable
private fun AccentSwitcher(current: Accent, onSelect: (Accent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Accent.entries.forEach { accent ->
            val selected = accent == current
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accent.swatch())
                    .clickable { onSelect(accent) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent.swatchOn()),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x2xl)
            .clip(Radius.xlPlus)
            .background(QsTheme.palette.surface),
    ) {
        content()
    }
}

@Composable
private fun GroupDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(QsTheme.palette.divider),
    )
}


@Composable
private fun retentionLabel(days: Int): String = when {
    days <= 0 -> stringResource(R.string.retention_forever)
    days == 1 -> stringResource(R.string.retention_day)
    else -> stringResource(R.string.retention_days, days)
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun readMediaImagesPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun Context.openAppSettings() {
    runCatching {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }
}

private fun StorageUsage.readableSize(): String {
    val kb = bytes / 1024
    val mb = kb / 1024
    return when {
        mb >= 1 -> "$mb MB"
        kb >= 1 -> "$kb KB"
        else -> "$bytes B"
    }
}