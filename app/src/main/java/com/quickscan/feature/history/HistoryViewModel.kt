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
    val stats: ScanStats = ScanStats(),
    val filter: ScanFilter = ScanFilter.All,
    val query: String = "",
    val searchOpen: Boolean = false,
    val totalUnfiltered: Int = 0,
) {
    val isEmpty: Boolean get() = groups.isEmpty() && totalUnfiltered == 0
    val hasNoMatches: Boolean get() = groups.isEmpty() && totalUnfiltered > 0
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(ScanFilter.All)
    private val query = MutableStateFlow("")
    private val searchOpen = MutableStateFlow(false)

    private val allScans = scanRepository.observeScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<HistoryUiState> = combine(
        allScans,
        scanRepository.observeStats(),
        filter,
        query,
        searchOpen,
    ) { scans, stats, activeFilter, activeQuery, isSearchOpen ->
        val searched = if (activeQuery.isBlank()) {
            scans
        } else {
            scans.filter { it.title.contains(activeQuery, ignoreCase = true) }
        }
        val filtered = searched.filter { activeFilter.matches(it.payloadType()) }

        HistoryUiState(
            groups = group(filtered),
            stats = stats,
            filter = activeFilter,
            query = activeQuery,
            searchOpen = isSearchOpen,
            totalUnfiltered = scans.size,
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