package com.quickscan.feature.history

import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import com.quickscan.data.local.ScanEntity
import com.quickscan.core.ui.component.QSIconTile
import com.quickscan.core.ui.component.LucideChevronRight
import com.quickscan.core.ui.component.LucideBookmark
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.QSSectionHeader
import com.quickscan.core.ui.component.QSTextField
import com.quickscan.core.ui.component.QSStat
import com.quickscan.core.ui.component.QSTabBar
import com.quickscan.core.ui.component.tabBarClearance
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var confirmDelete by remember { mutableStateOf(false) }
    var deletedCount by remember { mutableIntStateOf(0) }

    // Sharing a selection hands the raw text to another app; nothing is sent.
    val shareSelection = {
        scope.launch {
            val payload = viewModel.selectedPayloads()
            if (payload.isNotBlank()) {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, payload)
                        },
                        context.getString(R.string.share_scans_chooser),
                    ),
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(palette.bg)) {
    Column(modifier = Modifier.fillMaxSize()) {
        StatusBarSpacer()

        if (state.isSelecting) {
            SelectionBar(
                count = state.selection.size,
                onClose = viewModel::clearSelection,
                onSelectAll = viewModel::selectAll,
                onShare = { shareSelection() },
                onDelete = { confirmDelete = true },
            )
        } else {
            QSNavBar(
                title = stringResource(R.string.nav_history),
                onBack = onBack,
                actionIcon = LucideSearch,
                actionDescription = stringResource(R.string.search_history),
                onAction = { viewModel.toggleSearch() },
            )
        }

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
                    bottom = tabBarClearance(),
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

                if (state.pinned.isNotEmpty()) {
                    item(key = "pinned-header") {
                        QSSectionHeader(text = stringResource(R.string.pinned_section))
                    }
                    item(key = "pinned-card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Radius.xlPlus)
                                .background(palette.surface),
                        ) {
                            state.pinned.forEachIndexed { index, scan ->
                                if (index > 0) QSDivider(inset = 74.dp)
                                ScanRow(
                                    scan = scan,
                                    pinned = true,
                                    selected = scan.id in state.selection,
                                    selecting = state.isSelecting,
                                    onTap = { viewModel.onRowTap(scan.id) },
                                    onLongPress = { viewModel.onRowLongPress(scan.id) },
                                )
                            }
                        }
                    }
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
                                if (index > 0) QSDivider(inset = 74.dp)
                                ScanRow(
                                    scan = scan,
                                    pinned = false,
                                    selected = scan.id in state.selection,
                                    selecting = state.isSelecting,
                                    onTap = { viewModel.onRowTap(scan.id) },
                                    onLongPress = { viewModel.onRowLongPress(scan.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        QSTabBar(
            selected = TabDestination.History,
            onSelect = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = {
                    Text(
                        stringResource(
                            R.string.delete_selected_title,
                            state.selection.size,
                        ),
                    )
                },
                text = { Text(stringResource(R.string.delete_selected_body)) },
                confirmButton = {
                    Text(
                        text = stringResource(R.string.delete_selected),
                        color = palette.danger,
                        modifier = Modifier.clickable {
                            val count = state.selection.size
                            viewModel.deleteSelected {
                                deletedCount = count
                                confirmDelete = false
                            }
                        },
                    )
                },
                dismissButton = {
                    Text(
                        text = stringResource(R.string.setting_cancel),
                        modifier = Modifier.clickable { confirmDelete = false },
                    )
                },
                containerColor = palette.surface,
            )
        }
    }
    }

    if (deletedCount > 0) {
        LaunchedEffect(deletedCount) {
            Toast.makeText(
                context,
                context.getString(R.string.deleted_selected, deletedCount),
                Toast.LENGTH_SHORT,
            ).show()
            deletedCount = 0
        }
    }
}

/** One history row, shared by the favourites section and the date groups. */
@Composable
private fun ScanRow(
    scan: ScanEntity,
    pinned: Boolean,
    selected: Boolean,
    selecting: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val type = runCatching { PayloadType.valueOf(scan.type) }
        .getOrDefault(PayloadType.Text)
    val (tileBackground, tileInk) = payloadTileColors(type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(if (selected) palette.accentTint else Color.Transparent)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QSIconTile(
            icon = type.icon,
            tint = tileInk,
            background = tileBackground,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = scan.title,
                style = QsTheme.text.row15,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(type.labelRes) + " · " +
                    ScanDates.rowTime(scan.createdAt),
                style = QsTheme.text.rowSub12,
                color = palette.inkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when {
            selecting -> SelectionTick(selected = selected)
            pinned -> Icon(
                imageVector = LucideBookmark,
                contentDescription = null,
                tint = palette.accentTintInk,
                modifier = Modifier.size(16.dp),
            )

            else -> Icon(
                imageVector = LucideChevronRight,
                contentDescription = null,
                tint = palette.inkFaint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Toolbar shown instead of the nav bar while a selection is active. */
@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.surface)
                .combinedClickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LucideX,
                contentDescription = stringResource(R.string.setting_cancel),
                tint = palette.ink,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text = stringResource(R.string.selected_count, count),
            style = text.nav16,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )

        SelectionAction(
            icon = LucideCheckSquare,
            label = stringResource(R.string.select_all),
            onClick = onSelectAll,
        )
        SelectionAction(
            icon = LucideShare2,
            label = stringResource(R.string.share_selected),
            onClick = onShare,
        )
        SelectionAction(
            icon = LucideTrash2,
            label = stringResource(R.string.delete_selected),
            tint = palette.danger,
            onClick = onDelete,
        )
    }
}

@Composable
private fun SelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = QsTheme.palette.ink,
) {
    val palette = QsTheme.palette
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(palette.surface)
            .combinedClickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun SelectionTick(selected: Boolean) {
    val palette = QsTheme.palette
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) palette.accent else Color.Transparent)
            .border(
                width = if (selected) 0.dp else 2.dp,
                color = if (selected) palette.accent else palette.inkFaint,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = LucideCheck,
                contentDescription = null,
                tint = palette.accentOn,
                modifier = Modifier.size(14.dp),
            )
        }
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