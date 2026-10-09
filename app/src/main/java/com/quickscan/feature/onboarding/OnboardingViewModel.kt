package com.quickscan.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickscan.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val name: String = "",
    val saving: Boolean = false,
) {
    /** The avatar shows the first letter, or a neutral glyph until typed. */
    val initial: String
        get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "·"
}

/**
 * Emits once the preference write has landed; the screens navigate from that
 * rather than immediately, so the write cannot be cancelled by leaving.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /**
     * Navigating straight after asking to save used to cancel the write: this
     * ViewModel is scoped to its nav back-stack entry, so leaving the screen
     * tore down viewModelScope mid-write and onboarding replayed every launch.
     * Callers now wait for [events] before navigating.
     */
    private val _events = Channel<Unit>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameChanged(value: String) = _state.update { it.copy(name = value) }

    fun complete(name: String = _state.value.name) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            settingsRepository.setDisplayName(name.trim())
            settingsRepository.setOnboardingComplete(true)
            _state.update { it.copy(saving = false) }
            _events.send(Unit)
        }
    }
}