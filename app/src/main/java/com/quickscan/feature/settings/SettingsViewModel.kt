package com.quickscan.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickscan.core.release.ReleaseNotes
import com.quickscan.core.ui.theme.Accent
import com.quickscan.data.repository.ScanRepository
import com.quickscan.data.repository.SettingsRepository
import com.quickscan.data.repository.StorageUsage
import com.quickscan.data.repository.ThemeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val displayName: String = "",
    val autoDetect: Boolean = true,
    val preferFrontCamera: Boolean = false,
    val copyAutomatically: Boolean = false,
    val scanSound: Boolean = true,
    val vibrate: Boolean = true,
    val theme: ThemeState = ThemeState(),
    val retentionDays: Int = DEFAULT_RETENTION_DAYS,
    val storage: StorageUsage = StorageUsage(0, 0),
    val hasUnreadRelease: Boolean = false,
) {
    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
    }
}

private data class ScannerBehaviour(
    val autoDetect: Boolean,
    val preferFrontCamera: Boolean,
    val copyAutomatically: Boolean,
    val scanSound: Boolean,
    val vibrate: Boolean,
    val retentionDays: Int,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val storageUsage = MutableStateFlow(StorageUsage(0, 0))

    private val unreadRelease: Flow<Boolean> = settingsRepository.lastSeenRelease
        .map { ReleaseNotes.isUnseen(it) }

    private val behaviour: Flow<ScannerBehaviour> = combine(
        combine(
            settingsRepository.autoDetect,
            settingsRepository.preferFrontCamera,
            settingsRepository.copyAutomatically,
        ) { autoDetect, front, copy -> Triple(autoDetect, front, copy) },
        combine(
            settingsRepository.scanSound,
            settingsRepository.vibrateOnScan,
            settingsRepository.retentionDays,
        ) { sound, vibrate, retention -> Triple(sound, vibrate, retention) },
    ) { behaviourPrefs, feedbackPrefs ->
        ScannerBehaviour(
            autoDetect = behaviourPrefs.first,
            preferFrontCamera = behaviourPrefs.second,
            copyAutomatically = behaviourPrefs.third,
            scanSound = feedbackPrefs.first,
            vibrate = feedbackPrefs.second,
            retentionDays = feedbackPrefs.third,
        )
    }

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.displayName,
        behaviour,
        settingsRepository.themeState,
        storageUsage,
        unreadRelease,
    ) { name, scanner, theme, storage, unread ->
        SettingsUiState(
            displayName = name,
            autoDetect = scanner.autoDetect,
            preferFrontCamera = scanner.preferFrontCamera,
            copyAutomatically = scanner.copyAutomatically,
            scanSound = scanner.scanSound,
            vibrate = scanner.vibrate,
            theme = theme,
            retentionDays = scanner.retentionDays,
            storage = storage,
            hasUnreadRelease = unread,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch { storageUsage.value = scanRepository.storageUsage() }
    }

    fun setAutoDetect(value: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoDetect(value)
    }

    fun setPreferFrontCamera(value: Boolean) = viewModelScope.launch {
        settingsRepository.setPreferFrontCamera(value)
    }

    fun setCopyAutomatically(value: Boolean) = viewModelScope.launch {
        settingsRepository.setCopyAutomatically(value)
    }

    fun setScanSound(value: Boolean) = viewModelScope.launch {
        settingsRepository.setScanSound(value)
    }

    fun setVibrate(value: Boolean) = viewModelScope.launch {
        settingsRepository.setVibrateOnScan(value)
    }

    fun setDarkMode(value: Boolean) = viewModelScope.launch {
        settingsRepository.setDarkMode(value)
    }

    fun setLargerText(value: Boolean) = viewModelScope.launch {
        settingsRepository.setLargerText(value)
    }

    fun setAccent(accent: Accent) = viewModelScope.launch {
        settingsRepository.setAccent(accent)
    }

    fun setRetentionDays(value: Int) = viewModelScope.launch {
        settingsRepository.setRetentionDays(value)
        scanRepository.pruneExpired()
    }

    fun clearHistory() = viewModelScope.launch {
        scanRepository.clear()
        refreshStorage()
    }
}