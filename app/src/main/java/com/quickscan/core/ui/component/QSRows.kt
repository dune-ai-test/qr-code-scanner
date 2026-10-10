package com.quickscan.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickscan.core.ui.theme.Metrics
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space

/**
 * The 64dp row used for the onboarding value props and the "you can also scan"
 * suggestions: accent tile, title, subtitle, no chevron.
 */
@Composable
fun ValuePropRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    background: Color = QsTheme.palette.surface,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Metrics.listRowHeight)
            .clip(Radius.xl)
            .background(background)
            .padding(horizontal = Space.xl),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QSIconTile(
            icon = icon,
            tint = palette.accentTintInk,
            background = palette.accentTint,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = title, style = text.row15, color = palette.ink, maxLines = 1)
            Text(
                text = subtitle,
                style = text.rowSub12,
                color = palette.inkFaint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 42dp tinted icon tile that leads every list row. */
@Composable
fun QSIconTile(
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = Metrics.iconTile,
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(Radius.sm)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * The 64dp two-line row used by History and by the scanner's recent list.
 * [background] is transparent when the row sits inside a grouped card, which is
 * how the grouped History list is built.
 */
@Composable
fun QSListRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Metrics.listRowHeight)
            .clip(Radius.xl)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Space.xl),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QSIconTile(icon = icon, tint = iconTint, background = iconBackground)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = text.row15,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = text.rowSub12,
                    color = palette.inkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showChevron) {
            Icon(
                imageVector = LucideChevronRight,
                contentDescription = null,
                tint = palette.inkFaint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * The 62dp settings row. Trailing content differs per row — a switch, a value
 * with a chevron, or plain text — which is why it takes a slot.
 */
@Composable
fun QSSettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    background: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Metrics.settingRowHeight)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Space.xl),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QSIconTile(
            icon = icon,
            tint = palette.accentTintInk,
            background = palette.accentTint,
            size = Metrics.iconTileCompact,
            iconSize = 18.dp,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = title, style = text.row15, color = palette.ink, maxLines = 1)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = text.rowSub12,
                    color = palette.inkFaint,
                    maxLines = 2,
                )
            }
        }

        trailing?.invoke()
    }
}

/** Setting-row trailing slot: current value plus a chevron. */
@Composable
fun QSValueSlot(value: String, modifier: Modifier = Modifier) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style = text.body14,
            color = palette.inkMuted,
            maxLines = 1,
        )
        Icon(
            imageVector = LucideChevronRight,
            contentDescription = null,
            tint = palette.inkFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** A label + value line inside the grouped details cards. */
@Composable
fun QSDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Metrics.detailRowHeight)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = text.body14,
            color = palette.inkFaint,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = text.chip13.copy(fontWeight = FontWeight.Medium),
            color = palette.ink,
            maxLines = 1,
        )
    }
}

/** The 4%-black hairline that separates rows inside a grouped card. */
@Composable
fun QSDivider(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(1.dp)
            .background(QsTheme.palette.divider),
    )
}

/** All-caps 11pt section label used above grouped cards. */
@Composable
fun QSSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = QsTheme.text.label11,
        color = QsTheme.palette.inkFaint,
        modifier = modifier.padding(horizontal = Space.xl),
    )
}

/** A pill badge, used for payload types on the result hero. */
@Composable
fun QSBadge(
    icon: ImageVector?,
    label: String,
    modifier: Modifier = Modifier,
    background: Color = QsTheme.palette.accentTint,
    ink: Color = QsTheme.palette.accentTintInk,
) {
    val text = QsTheme.text

    Row(
        modifier = modifier
            .clip(Radius.pill)
            .background(background)
            .padding(horizontal = Space.md, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(text = label, style = text.rowSub12.copy(fontWeight = FontWeight.SemiBold), color = ink)
    }
}

/** One of the three History counters. */
@Composable
fun QSStat(value: String, label: String, modifier: Modifier = Modifier) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = modifier
            .height(Metrics.statTile)
            .clip(Radius.xl)
            .background(palette.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = value, style = text.title21, color = palette.ink, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = text.label11.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
            color = palette.inkFaint,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

