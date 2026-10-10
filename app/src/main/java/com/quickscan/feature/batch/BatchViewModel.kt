package com.quickscan.feature.batch

import androidx.lifecycle.ViewModel
import com.quickscan.core.qr.BatchPayloads
import com.quickscan.core.qr.QrStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BatchUiState(
    val input: String = "",
    /** Parsed once per edit. Reading this is what a recomposition does, and
     *  re-parsing sixty lines on every frame is not free. */
    val items: List<BatchPayloads.Item> = emptyList(),
    /** Non-blank lines the user actually pasted. */
    val pastedCount: Int = 0,
    val style: QrStyle = QrStyle.DEFAULT,
) {
    /**
     * Lines that became no code — duplicates, and anything past the cap.
     * Surfaced rather than swallowed, because a sheet with fewer codes than
     * lines pasted looks like a bug and is one unless it says so.
     */
    val droppedCount: Int get() = (pastedCount - items.size).coerceAtLeast(0)

    val overCap: Boolean get() = pastedCount > BatchPayloads.MAX_ITEMS

    val canBuild: Boolean get() = items.isNotEmpty()
}

/**
 * Holds the pasted list across rotation. Rendering the sheet belongs to the
 * screen, because it needs a canvas and must not happen per keystroke.
 */
@HiltViewModel
class BatchViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(BatchUiState())
    val state: StateFlow<BatchUiState> = _state.asStateFlow()

    fun setInput(value: String) = _state.update {
        it.copy(
            input = value,
            items = BatchPayloads.parse(value),
            pastedCount = value.lineSequence().count { line -> line.isNotBlank() },
        )
    }

    fun setStyle(style: QrStyle) = _state.update { it.copy(style = style) }
}