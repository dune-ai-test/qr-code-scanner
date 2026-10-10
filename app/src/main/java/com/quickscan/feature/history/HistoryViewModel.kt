package com.quickscan.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickscan.data.barcode.PayloadType
import com.quickscan.data.local.ScanEntity
import com.quickscan.core.ui.ScanDates
import com.quickscan.data.repository.ScanFilter
import com.quickscan.data.repository.ScanRepository
import com.quickscan.data.repository.ScanStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One date section in the grouped list. */
data class ScanGroup(
    val header: String,
    val scans: List<ScanEntity>,
)

data class HistoryUiState(
    val groups: List<ScanGroup> = emptyList(),
    val pinned: List<ScanEntity> = emptyList(),
    /** Ids currently ticked; empty means the list is browsing, not selecting. */
    val selection: Set<Long> = emptySet(),
    val stats: ScanStats = ScanStats(),
    val filter: ScanFilter = ScanFilter.All,
    val query: String = "",
    val searchOpen: Boolean = false,
    val totalUnfiltered: Int = 0,
    /** Ids the filter and search currently allow, for select-all. */
    val visibleIds: List<Long> = emptyList(),
) {
    val isSelecting: Boolean get() = selection.isNotEmpty()
    val isEmpty: Boolean get() = groups.isEmpty() && totalUnfiltered == 0
    val hasNoMatches: Boolean get() = groups.isEmpty() && totalUnfiltered > 0

    /** Rows that vanished while selected must not stay stuck ticked. */
    fun pruneSelection(): HistoryUiState =
        copy(selection = selection intersect visibleIds.toSet())
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(ScanFilter.All)
    private val query = MutableStateFlow("")
    private val searchOpen = MutableStateFlow(false)
    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    private val allScans = scanRepository.observeScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // combine() has typed overloads only up to five flows, so the controls
    // fold into one flow before being merged with the scans.
    private val controls: Flow<List<Any>> = combine(
        filter,
        query,
        searchOpen,
        selection,
    ) { activeFilter, activeQuery, isSearchOpen, selected ->
        listOf(activeFilter, activeQuery, isSearchOpen, selected)
    }

    val state: StateFlow<HistoryUiState> = combine(
        allScans,
        scanRepository.observeStats(),
        controls,
    ) { scans, stats, controlValues ->
        @Suppress("UNCHECKED_CAST")
        val activeFilter = controlValues[0] as ScanFilter
        @Suppress("UNCHECKED_CAST")
        val activeQuery = controlValues[1] as String
        @Suppress("UNCHECKED_CAST")
        val isSearchOpen = controlValues[2] as Boolean
        @Suppress("UNCHECKED_CAST")
        val selected = controlValues[3] as Set<Long>
        val searched = if (activeQuery.isBlank()) {
            scans
        } else {
            scans.filter { it.title.contains(activeQuery, ignoreCase = true) }
        }
        val filtered = searched.filter { activeFilter.matches(it.payloadType()) }

        HistoryUiState(
            groups = group(filtered.filterNot { it.isPinned }),
            pinned = filtered.filter { it.isPinned },
            stats = stats,
            filter = activeFilter,
            query = activeQuery,
            searchOpen = isSearchOpen,
            totalUnfiltered = scans.size,
            visibleIds = filtered.map { row -> row.id },
            selection = selected,
        ).pruneSelection()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    init {
        viewModelScope.launch { scanRepository.pruneExpired() }
    }

    fun setFilter(value: ScanFilter) {
        filter.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleSearch() {
        val open = !searchOpen.value
        searchOpen.value = open
        if (!open) query.value = ""
    }

    /** Long-press opens selection mode; a tap in it toggles one row. */
    fun onRowTap(id: Long) {
        selection.value = selection.value.let { current ->
            when {
                current.isEmpty() -> current
                id in current -> current - id
                else -> current + id
            }
        }
    }

    fun onRowLongPress(id: Long) {
        selection.value = if (id in selection.value) selection.value - id else selection.value + id
    }

    fun selectAll() {
        selection.value = state.value.visibleIds.toSet()
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun deleteSelected(onDone: () -> Unit) {
        val ids = selection.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            scanRepository.deleteAll(ids)
            selection.value = emptySet()
            onDone()
        }
    }

    suspend fun selectedPayloads(): String =
        scanRepository.rawValues(selection.value.toList()).joinToString("\n\n")

    fun clearHistory() {
        viewModelScope.launch { scanRepository.clear() }
    }

    private fun ScanEntity.payloadType(): PayloadType =
        runCatching { PayloadType.valueOf(type) }.getOrDefault(PayloadType.Text)

    /** Groups by day, preserving the descending order the query returns. */
    private fun group(scans: List<ScanEntity>): List<ScanGroup> =
        scans.groupBy { ScanDates.groupHeader(it.createdAt) }
            .map { (header, rows) -> ScanGroup(header, rows) }
            .toList()
}