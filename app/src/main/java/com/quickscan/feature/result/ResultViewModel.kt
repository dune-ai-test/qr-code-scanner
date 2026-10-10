package com.quickscan.feature.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickscan.data.barcode.ScannedPayload
import com.quickscan.data.local.ScanEntity
import com.quickscan.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val entity: ScanEntity? = null,
    val payload: ScannedPayload? = null,
    val pinned: Boolean = false,
    val loading: Boolean = true,
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val scanId: Long = checkNotNull(savedStateHandle["scanId"])

    private val _revealedPassword = MutableStateFlow(false)
    val revealedPassword: StateFlow<Boolean> = _revealedPassword.asStateFlow()

    val state: StateFlow<ResultUiState> = flow {
        emit(ResultUiState())
        val entity = scanRepository.find(scanId)
        if (entity == null) {
            emit(ResultUiState(loading = false))
            return@flow
        }
        val payload = scanRepository.parse(entity)
        emit(
            ResultUiState(
                entity = entity,
                payload = payload,
                pinned = entity.isPinned,
                loading = false,
            ),
        )
        scanRepository.observeScan(scanId).collect { row ->
            row?.let { emit(it.toState(payload)) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState())

    fun togglePasswordVisibility() {
        _revealedPassword.value = !_revealedPassword.value
    }

    fun togglePinned() {
        viewModelScope.launch { scanRepository.togglePinned(scanId) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            scanRepository.delete(scanId)
            onDeleted()
        }
    }

    private fun ScanEntity.toState(payload: ScannedPayload) = ResultUiState(
        entity = this,
        payload = payload,
        pinned = isPinned,
        loading = false,
    )
}

/** Convenience for screens that need both the row and the parsed payload. */
