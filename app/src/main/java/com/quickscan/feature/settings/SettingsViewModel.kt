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
    val continuousMode: Boolean = false,
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

/** What the scanner does with a detection. */
private data class ScanBehaviour(
    val autoDetect: Boolean,
    val continuousMode: Boolean,
    val preferFrontCamera: Boolean,
    val copyAutomatically: Boolean,
)

/** What the scanner says when it does. */
private data class FeedbackBehaviour(
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

    // combine() has typed overloads only up to five flows, so the seven
    // scanner preferences fold into two groups before being merged.
    private val behaviour: Flow<Pair<ScanBehaviour, FeedbackBehaviour>> = combine(
        combine(
            settingsRepository.autoDetect,
            settingsRepository.continuousMode,
            settingsRepository.preferFrontCamera,
            settingsRepository.copyAutomatically,
        ) { autoDetect, continuous, front, copy ->
            ScanBehaviour(
                autoDetect = autoDetect,
                continuousMode = continuous,
                preferFrontCamera = front,
                copyAutomatically = copy,
            )
        },
        combine(
            settingsRepository.scanSound,
            settingsRepository.vibrateOnScan,
            settingsRepository.retentionDays,
        ) { sound, vibrate, retention ->
            FeedbackBehaviour(sound, vibrate, retention)
        },
    ) { scan, feedback -> scan to feedback }

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.displayName,
        behaviour,
        settingsRepository.themeState,
        storageUsage,
        unreadRelease,
    ) { name, behaviours, theme, storage, unread ->
        val (scan, feedback) = behaviours
        SettingsUiState(
            displayName = name,
            autoDetect = scan.autoDetect,
            continuousMode = scan.continuousMode,
            preferFrontCamera = scan.preferFrontCamera,
            copyAutomatically = scan.copyAutomatically,
            scanSound = feedback.scanSound,
            vibrate = feedback.vibrate,
            theme = theme,
            retentionDays = feedback.retentionDays,
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

    fun setContinuousMode(value: Boolean) = viewModelScope.launch {
        settingsRepository.setContinuousMode(value)
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