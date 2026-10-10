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

/** A type the selection spans, with how many of its rows are ticked. */
data class TypeCount(
    val type: PayloadType,
    val count: Int,
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
    /** Types present in the selection, each with its ticked count. */
    val selectionByType: List<TypeCount> = emptyList(),
    /** Types the bulk actions apply to; null means every ticked type. */
    val scope: Set<PayloadType>? = null,
    /** The ticked ids the bulk actions actually reach. */
    val scopedSelection: Set<Long> = emptySet(),
    /** How many of [scopedSelection] are already pinned. */
    val scopedPinned: Int = 0,
) {
    val isSelecting: Boolean get() = selection.isNotEmpty()
    val isEmpty: Boolean get() = groups.isEmpty() && totalUnfiltered == 0
    val hasNoMatches: Boolean get() = groups.isEmpty() && totalUnfiltered > 0

    /**
     * True when every row in scope is pinned, which turns the pin action into
     * an unpin. A mixed selection pins, because the useful reading of "pin
     * these" when some are already pinned is "make them all pinned".
     */
    val allPinned: Boolean
        get() = scopedSelection.isNotEmpty() && scopedPinned == scopedSelection.size
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(ScanFilter.All)
    private val query = MutableStateFlow("")
    private val searchOpen = MutableStateFlow(false)
    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    /** Types the bulk actions are narrowed to; null means no narrowing. */
    private val scope = MutableStateFlow<Set<PayloadType>?>(null)

    private val allScans = scanRepository.observeScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // combine() has typed overloads only up to five flows, so the controls
    // fold into one flow before being merged with the scans.
    // One element is nullable — the type scope is null for "no narrowing" —
    // so the fold is List<Any?>, and the casts below are what turn each entry
    // back into its own type.
    private val controls: Flow<List<Any?>> = combine(
        filter,
        query,
        searchOpen,
        selection,
        scope,
    ) { activeFilter, activeQuery, isSearchOpen, selected, activeScope ->
        listOf(activeFilter, activeQuery, isSearchOpen, selected, activeScope)
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
        @Suppress("UNCHECKED_CAST")
        val activeScope = controlValues[4] as Set<PayloadType>?
        val searched = if (activeQuery.isBlank()) {
            scans
        } else {
            scans.filter { it.title.contains(activeQuery, ignoreCase = true) }
        }
        val filtered = searched.filter { activeFilter.matches(it.payloadType()) }
        val visibleIds = filtered.map { row -> row.id }

        // Rows that vanished while ticked must not stay selected, and every
        // derived number is computed from what survives, not from the raw
        // selection — otherwise the toolbar could claim a count the actions
        // cannot reach.
        val kept = selected intersect visibleIds.toSet()
        val typeById = filtered.associate { row -> row.id to row.payloadType() }
        // eachCount() has no defined iteration order, so the chips are put
        // back into enum order; otherwise the row reshuffles between emissions.
        val byType = filtered.filter { row -> row.id in kept }
            .groupingBy { row -> row.payloadType() }
            .eachCount()
            .map { (type, count) -> TypeCount(type, count) }
            .sortedBy { it.type.ordinal }

        // A scope naming a type with no ticked rows left would leave the
        // toolbar with nothing to act on, so it falls back to all of them.
        val survivingScope = activeScope?.takeIf { wanted ->
            byType.any { it.type in wanted }
        }
        val scoped = idsInScope(kept, typeById, survivingScope)
        val scopedPinned = filtered.count { row -> row.isPinned && row.id in scoped }

        HistoryUiState(
            groups = group(filtered.filterNot { it.isPinned }),
            pinned = filtered.filter { it.isPinned },
            stats = stats,
            filter = activeFilter,
            query = activeQuery,
            searchOpen = isSearchOpen,
            totalUnfiltered = scans.size,
            visibleIds = visibleIds,
            selection = kept,
            selectionByType = byType,
            scope = survivingScope,
            scopedSelection = scoped,
            scopedPinned = scopedPinned,
        )
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
        scope.value = null
    }

    /**
     * Narrows the bulk actions to one type, or widens them back out. Turning a
     * type off is what turns "Delete 9" into "delete only the Wi-Fi ones".
     */
    fun toggleTypeScope(type: PayloadType) {
        val present = state.value.selectionByType.mapTo(mutableSetOf()) { it.type }
        scope.value = nextTypeScope(present, scope.value, type)
    }

    /**
     * Pins or unpins everything the scope reaches. The selection survives: a
     * user who pins three rows usually still wants to share or delete them.
     */
    fun setSelectedPinned(pinned: Boolean) {
        val ids = state.value.scopedSelection.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch { scanRepository.setPinned(ids, pinned) }
    }

    fun deleteSelected(onDone: () -> Unit) {
        val ids = state.value.scopedSelection.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            scanRepository.deleteAll(ids)
            selection.value = emptySet()
            scope.value = null
            onDone()
        }
    }

    suspend fun selectedPayloads(): String =
        scanRepository.rawValues(state.value.scopedSelection.toList())
            .joinToString("\n\n")

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