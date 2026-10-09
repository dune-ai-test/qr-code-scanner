package com.quickscan.feature.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.R
import com.quickscan.core.ui.ScanDates
import com.quickscan.core.ui.headline
import com.quickscan.core.ui.labelRes
import com.quickscan.core.ui.component.BannerTone
import com.quickscan.core.ui.component.LucideCopy
import com.quickscan.core.ui.component.LucideEllipsis
import com.quickscan.core.ui.component.LucideExternalLink
import com.quickscan.core.ui.component.LucideEye
import com.quickscan.core.ui.component.LucideEyeOff
import com.quickscan.core.ui.component.LucideLock
import com.quickscan.core.ui.component.LucideShare2
import com.quickscan.core.ui.component.LucideShieldCheck
import com.quickscan.core.ui.component.LucideWifi
import com.quickscan.core.ui.component.LucideLink
import com.quickscan.core.ui.component.QSBadge
import com.quickscan.core.ui.component.QSBanner
import com.quickscan.core.ui.component.QSDetailRow
import com.quickscan.core.ui.component.QSDivider
import com.quickscan.core.ui.component.QSIconTile
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSSquareAction
import com.quickscan.core.ui.component.QSTabBar
import com.quickscan.core.ui.component.QrCodeView
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.TabDestination
import com.quickscan.core.ui.icon
import com.quickscan.data.local.ScanEntity
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space
import com.quickscan.data.barcode.PayloadType
import com.quickscan.data.barcode.ScannedPayload
import com.quickscan.data.safety.SafetyVerdict

@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onTabSelected: (TabDestination) -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val passwordVisible by viewModel.revealedPassword.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val palette = QsTheme.palette
    val text = QsTheme.text

    val entity = state.entity
    val payload = state.payload

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        StatusBarSpacer()

        QSNavBar(
            title = stringResource(R.string.nav_scan_result),
            onBack = onBack,
            actionIcon = LucideEllipsis,
            actionDescription = stringResource(R.string.more_actions),
            onAction = { payload?.let { context.sharePayload(it) } },
        )

        if (payload == null || entity == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.empty_search_title),
                    style = text.body15,
                    color = palette.inkFaint,
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.x2xl),
            verticalArrangement = Arrangement.spacedBy(Space.x2xl),
        ) {
            when (payload) {
                is ScannedPayload.Url -> UrlHero(
                    raw = payload.raw,
                    safety = state.safety,
                    scannedAt = entity.createdAt,
                )

                is ScannedPayload.Wifi -> WifiHero(
                    payload = payload,
                    scannedAt = entity.createdAt,
                    passwordVisible = passwordVisible,
                    onTogglePassword = viewModel::togglePasswordVisibility,
                )

                is ScannedPayload.Contact -> ContactHero(
                    payload = payload,
                    scannedAt = entity.createdAt,
                )

                else -> CodeHero(
                    raw = payload.raw,
                    type = payload.type,
                    scannedAt = entity.createdAt,
                )
            }

            ActionRow(payload = payload)

            DetailsCard(entity = entity, payload = payload)

            Text(
                text = stringResource(R.string.delete_from_history),
                style = text.body14.copy(fontWeight = FontWeight.Medium),
                color = palette.danger,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.lgPlus)
                    .background(palette.surface)
                    .clickable { viewModel.delete(onBack) }
                    .padding(vertical = 17.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Space.md))
        }

        QSTabBar(selected = TabDestination.Scan, onSelect = onTabSelected)
    }
}

/** The URL result: decoded address, safety verdict and an open action. */
@Composable
private fun UrlHero(
    raw: String,
    safety: SafetyVerdict?,
    scannedAt: Long,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.hero)
                .background(palette.surface)
                .padding(horizontal = Space.section, vertical = Space.x5xl),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.xl),
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(Radius.lg)
                        .background(palette.surfaceElevated)
                        .padding(10.dp),
                ) {
                    QrCodeView(
                        content = raw,
                        foreground = palette.ink,
                        background = palette.surfaceElevated,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                QSBadge(
                    icon = LucideLink,
                    label = stringResource(R.string.type_website),
                )

                Text(
                    text = raw,
                    style = text.title18,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.scanned_at, ScanDates.detailTime(scannedAt)),
                    style = text.rowSub12,
                    color = palette.inkFaint,
                )
            }
        }

        safety?.let { verdict ->
            QSBanner(
                icon = LucideShieldCheck,
                message = verdict.headline,
                tone = if (verdict.isSafe) BannerTone.Success else BannerTone.Warning,
            )
            if (!verdict.isSafe) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    verdict.reasons.forEach { reason ->
                        Text(
                            text = stringResource(reason.labelRes),
                            style = QsTheme.text.rowSub12,
                            color = palette.warnInk,
                            modifier = Modifier.padding(start = Space.xl),
                        )
                    }
                }
            }
        }
    }
}

/** The Wi-Fi result: network name, a revealable password and a join action. */
@Composable
private fun WifiHero(
    payload: ScannedPayload.Wifi,
    scannedAt: Long,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.hero)
                .background(palette.surface)
                .padding(Space.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            QSIconTile(
                icon = LucideWifi,
                tint = palette.accentTintInk,
                background = palette.accentTint,
                size = 76.dp,
                iconSize = 32.dp,
            )
            Text(text = payload.ssid, style = text.title21, color = palette.ink)
            Text(
                text = payload.displayMeta,
                style = text.rowSub12,
                color = palette.inkFaint,
            )
        }

        if (payload.password.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.xl)
                    .background(palette.surface)
                    .padding(horizontal = Space.xl, vertical = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = LucideLock,
                    contentDescription = null,
                    tint = palette.inkFaint,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = if (passwordVisible) {
                        payload.password
                    } else {
                        "•".repeat(payload.password.length.coerceAtMost(12))
                    },
                    style = text.row15,
                    color = palette.ink,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (passwordVisible) LucideEyeOff else LucideEye,
                    contentDescription = stringResource(
                        if (passwordVisible) R.string.hide_password else R.string.reveal_password,
                    ),
                    tint = palette.accentTintInk,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onTogglePassword),
                )
            }
        }

        Text(
            text = stringResource(R.string.scanned_at, ScanDates.detailTime(scannedAt)),
            style = text.rowSub12,
            color = palette.inkFaint,
        )
    }
}

@Composable
private fun ContactHero(payload: ScannedPayload.Contact, scannedAt: Long) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.hero)
                .background(palette.surface)
                .padding(Space.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            QSIconTile(
                icon = payload.type.icon,
                tint = palette.accentTintInk,
                background = palette.accentTint,
                size = 76.dp,
                iconSize = 32.dp,
            )
            Text(text = payload.name, style = text.title21, color = palette.ink)
            listOfNotNull(payload.phone, payload.email).takeIf { it.isNotEmpty() }
                ?.joinToString(" · ")
                ?.let { meta ->
                    Text(text = meta, style = text.rowSub12, color = palette.inkFaint)
                }
        }
        Text(
            text = stringResource(R.string.scanned_at, ScanDates.detailTime(scannedAt)),
            style = text.rowSub12,
            color = palette.inkFaint,
        )
    }
}

/** Everything that is not a link or a network renders as a plain code. */
@Composable
private fun CodeHero(raw: String, type: PayloadType, scannedAt: Long) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.hero)
                .background(palette.surface)
                .padding(horizontal = Space.section, vertical = Space.x5xl),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.xl),
            ) {
                QSIconTile(
                    icon = type.icon,
                    tint = palette.accentTintInk,
                    background = palette.surfaceElevated,
                    size = 120.dp,
                    iconSize = 48.dp,
                )
                QSBadge(
                    icon = null,
                    label = stringResource(type.labelRes),
                )
                Text(
                    text = raw,
                    style = text.nav16,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = stringResource(R.string.scanned_at, ScanDates.detailTime(scannedAt)),
            style = text.rowSub12,
            color = palette.inkFaint,
        )
    }
}

/** The action set is chosen by payload type, so each result does what fits. */
@Composable
private fun ActionRow(payload: ScannedPayload) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QSSquareAction(
            icon = LucideCopy,
            contentDescription = stringResource(R.string.copy_action),
            onClick = { context.copyToClipboard(payload.raw) },
        )

        when (payload) {
            is ScannedPayload.Url -> {
                QSPrimaryButton(
                    label = stringResource(R.string.open_link),
                    onClick = { context.openUrl(payload.url) },
                    icon = LucideExternalLink,
                    modifier = Modifier.weight(1f),
                )
                QSSquareAction(
                    icon = LucideShare2,
                    contentDescription = stringResource(R.string.share_action),
                    onClick = { context.sharePayload(payload) },
                )
            }

            is ScannedPayload.Wifi -> {
                QSPrimaryButton(
                    label = stringResource(R.string.join_network),
                    onClick = { context.joinWifi(payload) },
                    icon = LucideWifi,
                    modifier = Modifier.weight(1f),
                )
                QSSquareAction(
                    icon = LucideCopy,
                    contentDescription = stringResource(R.string.copy_action),
                    onClick = { context.copyToClipboard(payload.password) },
                )
            }

            is ScannedPayload.Contact -> {
                QSPrimaryButton(
                    label = stringResource(
                        if (payload.phone != null) R.string.dial_contact else R.string.share_action,
                    ),
                    onClick = {
                        payload.phone?.let { context.dial(it) } ?: context.sharePayload(payload)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            else -> {
                QSPrimaryButton(
                    label = stringResource(R.string.share_action),
                    onClick = { context.sharePayload(payload) },
                    icon = LucideShare2,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(
    entity: ScanEntity,
    payload: ScannedPayload,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.xlPlus)
            .background(QsTheme.palette.surface),
    ) {
        if (payload is ScannedPayload.Wifi) {
            // Networks get their own fields instead of the generic type/length pair.
            QSDetailRow(
                label = stringResource(R.string.wifi_ssid_label),
                value = payload.ssid,
            )
            QSDivider()
            QSDetailRow(
                label = stringResource(R.string.wifi_security_label),
                value = payload.securityLabel,
            )
        } else {
            QSDetailRow(
                label = stringResource(R.string.detail_type),
                value = stringResource(
                    when (payload) {
                        is ScannedPayload.Url -> R.string.detail_website_url
                        is ScannedPayload.Contact -> R.string.detail_contact_card
                        is ScannedPayload.Product -> R.string.detail_barcode_number
                        is ScannedPayload.Text -> R.string.detail_plain_text
                        is ScannedPayload.Wifi -> R.string.type_wifi
                    },
                ),
            )
            QSDivider()
            QSDetailRow(
                label = stringResource(R.string.detail_length),
                value = stringResource(R.string.characters, payload.raw.length),
            )
        }
        QSDivider()
        QSDetailRow(
            label = stringResource(R.string.detail_scanned),
            value = ScanDates.detailTime(entity.createdAt),
        )
        QSDivider()
        QSDetailRow(
            label = stringResource(R.string.detail_source),
            value = stringResource(
                when (entity.source) {
                    "Image" -> R.string.source_image
                    "Manual" -> R.string.source_manual
                    else -> R.string.source_camera
                },
            ),
        )
    }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("QuickScan", text))
    Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
}

private fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(packageManager) == null) {
        Toast.makeText(this, R.string.error_no_browser, Toast.LENGTH_SHORT).show()
        return
    }
    startActivity(intent)
}

private fun Context.sharePayload(payload: ScannedPayload) {
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, payload.headline)
            },
            getString(R.string.share_action),
        ),
    )
}

/**
 * Android 10 and up can add a network straight from a Wi-Fi suggestion; older
 * releases, and devices without the settings screen, fall back to Wi-Fi settings.
 */
private fun Context.joinWifi(payload: ScannedPayload.Wifi) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val builder = WifiNetworkSuggestion.Builder()
            .setSsid(payload.ssid)
            .setIsHiddenSsid(payload.hidden)
        // An open network carries no passphrase; leaving it unset is correct.
        if (payload.password.isNotEmpty()) {
            builder.setWpa2Passphrase(payload.password)
        }

        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
            putParcelableArrayListExtra(
                Settings.EXTRA_WIFI_NETWORK_LIST,
                arrayListOf(builder.build()),
            )
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            return
        }
    }
    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
}

private fun Context.dial(number: String) {
    runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) }
        .onFailure {
            Toast.makeText(this, R.string.error_no_camera_app, Toast.LENGTH_SHORT).show()
        }
}