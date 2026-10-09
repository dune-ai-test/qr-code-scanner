package com.quickscan.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.R
import com.quickscan.core.ui.ScanDates
import com.quickscan.core.ui.icon
import com.quickscan.core.ui.labelRes
import com.quickscan.core.ui.payloadTileColors
import com.quickscan.core.ui.component.LucideLink
import com.quickscan.core.ui.component.LucideScanLine
import com.quickscan.core.ui.component.LucideScanSearch
import com.quickscan.core.ui.component.LucideSearch
import com.quickscan.core.ui.component.LucideUser
import com.quickscan.core.ui.component.LucideWifi
import com.quickscan.core.ui.component.QSChip
import com.quickscan.core.ui.component.QSDivider
import com.quickscan.core.ui.component.QSListRow
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.QSSectionHeader
import com.quickscan.core.ui.component.QSTextField
import com.quickscan.core.ui.component.QSStat
import com.quickscan.core.ui.component.QSTabBar
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.component.TabDestination
import com.quickscan.core.ui.component.ValuePropRow
import com.quickscan.core.ui.theme.Metrics
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space
import com.quickscan.data.barcode.PayloadType
import com.quickscan.data.repository.ScanFilter

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenScan: (Long) -> Unit,
    onStartScanning: () -> Unit,
    onTabSelected: (TabDestination) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        StatusBarSpacer()

        QSNavBar(
            title = stringResource(R.string.nav_history),
            onBack = onBack,
            actionIcon = LucideSearch,
            actionDescription = stringResource(R.string.search_history),
            onAction = { viewModel.toggleSearch() },
        )

        when {
            state.isEmpty -> EmptyHistory(
                onStartScanning = onStartScanning,
                modifier = Modifier.weight(1f),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Space.x2xl,
                    end = Space.x2xl,
                    top = Space.x2xl,
                    bottom = Space.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.xxl),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        QSStat(
                            value = state.stats.total.toString(),
                            label = stringResource(R.string.stat_total_scans),
                            modifier = Modifier.weight(1f),
                        )
                        QSStat(
                            value = state.stats.thisWeek.toString(),
                            label = stringResource(R.string.stat_this_week),
                            modifier = Modifier.weight(1f),
                        )
                        QSStat(
                            value = state.stats.links.toString(),
                            label = stringResource(R.string.stat_links),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (state.searchOpen) {
                    item {
                        QSTextField(
                            value = state.query,
                            onValueChange = viewModel::setQuery,
                            label = stringResource(R.string.search_hint),
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    ) {
                        FilterChips(
                            selected = state.filter,
                            onSelect = viewModel::setFilter,
                        )
                    }
                }

                if (state.hasNoMatches) {
                    item { NoMatches() }
                }

                state.groups.forEach { group ->
                    item(key = "header-${group.header}") {
                        QSSectionHeader(text = group.header)
                    }
                    item(key = "card-${group.header}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Radius.xlPlus)
                                .background(palette.surface),
                        ) {
                            group.scans.forEachIndexed { index, scan ->
                                if (index > 0) {
                                    QSDivider(inset = 74.dp)
                                }
                                val type = runCatching { PayloadType.valueOf(scan.type) }
                                    .getOrDefault(PayloadType.Text)
                                val (tileBackground, tileInk) = payloadTileColors(type)

                                QSListRow(
                                    icon = type.icon,
                                    iconTint = tileInk,
                                    iconBackground = tileBackground,
                                    title = scan.title,
                                    subtitle = "${stringResource(type.labelRes)} · " +
                                        ScanDates.rowTime(scan.createdAt),
                                    onClick = { onOpenScan(scan.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        QSTabBar(selected = TabDestination.History, onSelect = onTabSelected)
    }
}

@Composable
private fun FilterChips(selected: ScanFilter, onSelect: (ScanFilter) -> Unit) {
    QSChip(
        label = stringResource(R.string.filter_all),
        selected = selected == ScanFilter.All,
        onClick = { onSelect(ScanFilter.All) },
    )
    QSChip(
        label = stringResource(R.string.filter_links),
        selected = selected == ScanFilter.Links,
        onClick = { onSelect(ScanFilter.Links) },
    )
    QSChip(
        label = stringResource(R.string.filter_wifi),
        selected = selected == ScanFilter.Wifi,
        onClick = { onSelect(ScanFilter.Wifi) },
    )
    QSChip(
        label = stringResource(R.string.filter_text),
        selected = selected == ScanFilter.Text,
        onClick = { onSelect(ScanFilter.Text) },
    )
}

/** The empty state the mockups show before a first scan. */
@Composable
private fun EmptyHistory(
    onStartScanning: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Space.x2xl),
    ) {
        Spacer(Modifier.height(54.dp))

        Column(
            modifier = Modifier.padding(horizontal = Space.x2xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xxl),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.hero)
                    .background(palette.surface)
                    .padding(horizontal = Space.x5xl, vertical = Space.hero),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.xxl),
            ) {
                Box(
                    modifier = Modifier
                        .size(Metrics.emptyRing)
                        .shadow(6.dp, Radius.capsule, spotColor = palette.shadow)
                        .clip(Radius.capsule)
                        .background(palette.surfaceElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = LucideScanSearch,
                        contentDescription = null,
                        tint = palette.accentTintInk,
                        modifier = Modifier.size(52.dp),
                    )
                }

                Text(
                    text = stringResource(R.string.empty_no_scans_title),
                    style = text.title21,
                    color = palette.ink,
                )
                Text(
                    text = stringResource(R.string.empty_no_scans_body),
                    style = text.body14,
                    color = palette.inkMuted,
                    textAlign = TextAlign.Center,
                )
                QSPrimaryButton(
                    label = stringResource(R.string.empty_scan_first),
                    onClick = onStartScanning,
                    icon = LucideScanLine,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            QSSectionHeader(text = stringResource(R.string.empty_you_can_also_scan))

            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                ValuePropRow(
                    icon = LucideWifi,
                    title = stringResource(R.string.suggestion_wifi_title),
                    subtitle = stringResource(R.string.suggestion_wifi_subtitle),
                )
                ValuePropRow(
                    icon = LucideLink,
                    title = stringResource(R.string.suggestion_link_title),
                    subtitle = stringResource(R.string.suggestion_link_subtitle),
                )
                ValuePropRow(
                    icon = LucideUser,
                    title = stringResource(R.string.suggestion_contact_title),
                    subtitle = stringResource(R.string.suggestion_contact_subtitle),
                )
            }
        }

        Spacer(Modifier.height(Space.x2xl))
    }
}

@Composable
private fun NoMatches() {
    val palette = QsTheme.palette
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Text(
            text = stringResource(R.string.empty_search_title),
            style = QsTheme.text.nav16,
            color = palette.ink,
        )
        Text(
            text = stringResource(R.string.empty_search_body),
            style = QsTheme.text.body14,
            color = palette.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}