package com.quickscan.data.repository

import com.quickscan.core.ui.theme.Accent
import com.quickscan.data.local.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** The three inputs the theme is built from. */
data class ThemeState(
    val darkMode: Boolean = false,
    val accent: Accent = Accent.Ocean,
    val largerText: Boolean = false,
)

/** Everything the scanner needs to behave the way the user configured. */
data class ScannerPreferences(
    val autoDetect: Boolean = true,
    /** Stay in the viewfinder after a detection instead of opening the result. */
    val continuousMode: Boolean = false,
    val copyAutomatically: Boolean = false,
    val scanSound: Boolean = true,
    val vibrateOnScan: Boolean = true,
    val preferFrontCamera: Boolean = false,
    val retentionDays: Int = SettingsStore.DEFAULT_RETENTION_DAYS,
)

class SettingsRepository(private val store: SettingsStore) {

    val onboardingComplete: Flow<Boolean> = store.onboardingComplete
    val displayName: Flow<String> = store.displayName

    val autoDetect: Flow<Boolean> = store.autoDetect
    val continuousMode: Flow<Boolean> = store.continuousMode
    val copyAutomatically: Flow<Boolean> = store.copyAutomatically
    val scanSound: Flow<Boolean> = store.scanSound
    val vibrateOnScan: Flow<Boolean> = store.vibrateOnScan
    val preferFrontCamera: Flow<Boolean> = store.preferFrontCamera
    val retentionDays: Flow<Int> = store.retentionDays
    val darkMode: Flow<Boolean> = store.darkMode
    val accentName: Flow<String> = store.accentName
    val largerText: Flow<Boolean> = store.largerText

    val lastSeenRelease: Flow<String?> = store.lastSeenRelease

    // combine() has typed overloads only up to five flows, so this folds into
    // two groups first rather than spilling into the untyped vararg overload.
    val scannerPreferences: Flow<ScannerPreferences> = combine(
        combine(
            store.autoDetect,
            store.continuousMode,
            store.copyAutomatically,
            store.scanSound,
        ) { autoDetect, continuous, copy, sound ->
            ScannerPreferences(
                autoDetect = autoDetect,
                continuousMode = continuous,
                copyAutomatically = copy,
                scanSound = sound,
            )
        },
        combine(
            store.vibrateOnScan,
            store.preferFrontCamera,
            store.retentionDays,
        ) { vibrate, front, days ->
            Triple(vibrate, front, days)
        },
    ) { base, (vibrate, front, days) ->
        base.copy(
            vibrateOnScan = vibrate,
            preferFrontCamera = front,
            retentionDays = days,
        )
    }

    val themeState: Flow<ThemeState> = combine(
        store.darkMode,
        store.accentName,
        store.largerText,
    ) { dark, accentName, larger ->
        ThemeState(
            darkMode = dark,
            accent = accentName.toAccent(),
            largerText = larger,
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) = store.setOnboardingComplete(value)

    suspend fun setDisplayName(value: String) = store.setDisplayName(value)

    suspend fun setAutoDetect(value: Boolean) = store.setAutoDetect(value)

    suspend fun setContinuousMode(value: Boolean) = store.setContinuousMode(value)

    suspend fun setCopyAutomatically(value: Boolean) = store.setCopyAutomatically(value)

    suspend fun setScanSound(value: Boolean) = store.setScanSound(value)

    suspend fun setVibrateOnScan(value: Boolean) = store.setVibrateOnScan(value)

    suspend fun setPreferFrontCamera(value: Boolean) = store.setPreferFrontCamera(value)

    suspend fun setDarkMode(value: Boolean) = store.setDarkMode(value)

    suspend fun setAccent(accent: Accent) = store.setAccent(accent.name)

    suspend fun setLargerText(value: Boolean) = store.setLargerText(value)

    suspend fun setRetentionDays(value: Int) = store.setRetentionDays(value)

    suspend fun setLastSeenRelease(version: String) = store.setLastSeenRelease(version)
}

private fun String.toAccent(): Accent =
    Accent.entries.firstOrNull { it.name == this } ?: Accent.Ocean