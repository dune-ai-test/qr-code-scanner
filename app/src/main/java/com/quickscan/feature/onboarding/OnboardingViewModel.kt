package com.quickscan.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickscan.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onNameChanged(value: String) = _state.update { it.copy(name = value) }

    fun complete(name: String = _state.value.name) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            settingsRepository.setDisplayName(name.trim())
            settingsRepository.setOnboardingComplete(true)
            _state.update { it.copy(saving = false) }
        }
    }
}