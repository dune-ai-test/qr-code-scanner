package com.quickscan.feature.whatsnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickscan.core.release.ReleaseNotes
import com.quickscan.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WhatsNewUiState(
    val entries: List<ReleaseNotes.Entry> = ReleaseNotes.entries,
    val hasUnread: Boolean = false,
)

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WhatsNewUiState())
    val state: StateFlow<WhatsNewUiState> = _state.asStateFlow()

    val hasUnread: StateFlow<Boolean> = settingsRepository.lastSeenRelease
        .map { ReleaseNotes.isUnseen(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Opening the screen counts as reading it. */
    fun markRead() {
        viewModelScope.launch {
            settingsRepository.setLastSeenRelease(ReleaseNotes.current.version)
        }
    }
}
