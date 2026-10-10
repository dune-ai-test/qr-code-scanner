package com.quickscan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Every user preference, stored on-device. */
class SettingsStore(private val context: Context) {

    val onboardingComplete: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING] ?: false }

    val displayName: Flow<String> =
        context.dataStore.data.map { it[KEY_NAME] ?: "" }

    val autoDetect: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_AUTO_DETECT] ?: true }

    /**
     * Whether a detection stays in the viewfinder instead of opening the
     * result. Off by default, so the one-code-at-a-time behaviour nobody has
     * asked to change stays the default.
     */
    val continuousMode: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_CONTINUOUS_MODE] ?: false }

    val copyAutomatically: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_COPY_AUTOMATICALLY] ?: false }

    val scanSound: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SCAN_SOUND] ?: true }

    val vibrateOnScan: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_VIBRATE] ?: true }

    /** "BACK" or "FRONT". */
    val preferFrontCamera: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_FRONT_CAMERA] ?: false }

    val darkMode: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_DARK_MODE] ?: false }

    /** Accent name; see [com.quickscan.core.ui.theme.Accent]. */
    val accentName: Flow<String> =
        context.dataStore.data.map { it[KEY_ACCENT] ?: "Ocean" }

    val largerText: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_LARGER_TEXT] ?: false }

    /** Newest release-notes version the reader has seen, or null. */
    val lastSeenRelease: Flow<String?> =
        context.dataStore.data.map { it[KEY_LAST_SEEN_RELEASE] }

    /** Days of history to keep, or 0 for forever. */
    val retentionDays: Flow<Int> =
        context.dataStore.data.map { it[KEY_RETENTION] ?: DEFAULT_RETENTION_DAYS }

    suspend fun setOnboardingComplete(value: Boolean) = put(KEY_ONBOARDING, value)

    suspend fun setDisplayName(value: String) = put(KEY_NAME, value)

    suspend fun setAutoDetect(value: Boolean) = put(KEY_AUTO_DETECT, value)

    suspend fun setContinuousMode(value: Boolean) = put(KEY_CONTINUOUS_MODE, value)

    suspend fun setCopyAutomatically(value: Boolean) = put(KEY_COPY_AUTOMATICALLY, value)

    suspend fun setScanSound(value: Boolean) = put(KEY_SCAN_SOUND, value)

    suspend fun setVibrateOnScan(value: Boolean) = put(KEY_VIBRATE, value)

    suspend fun setPreferFrontCamera(value: Boolean) = put(KEY_FRONT_CAMERA, value)

    suspend fun setDarkMode(value: Boolean) = put(KEY_DARK_MODE, value)

    suspend fun setAccent(value: String) = put(KEY_ACCENT, value)

    suspend fun setLargerText(value: Boolean) = put(KEY_LARGER_TEXT, value)

    suspend fun setRetentionDays(value: Int) = put(KEY_RETENTION, value)

    suspend fun setLastSeenRelease(value: String) = put(KEY_LAST_SEEN_RELEASE, value)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS = 30

        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_complete")
        private val KEY_NAME = stringPreferencesKey("display_name")
        private val KEY_AUTO_DETECT = booleanPreferencesKey("auto_detect")
        private val KEY_CONTINUOUS_MODE = booleanPreferencesKey("continuous_mode")
        private val KEY_COPY_AUTOMATICALLY = booleanPreferencesKey("copy_automatically")
        private val KEY_SCAN_SOUND = booleanPreferencesKey("scan_sound")
        private val KEY_VIBRATE = booleanPreferencesKey("vibrate_on_scan")
        private val KEY_FRONT_CAMERA = booleanPreferencesKey("prefer_front_camera")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_ACCENT = stringPreferencesKey("accent")
        private val KEY_LARGER_TEXT = booleanPreferencesKey("larger_text")
        private val KEY_RETENTION = intPreferencesKey("retention_days")
        private val KEY_LAST_SEEN_RELEASE = stringPreferencesKey("last_seen_release")
    }
}