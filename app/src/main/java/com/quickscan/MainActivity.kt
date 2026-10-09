package com.quickscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.core.ui.theme.QuickScanTheme
import com.quickscan.data.repository.SettingsRepository
import com.quickscan.data.repository.ThemeState
import com.quickscan.nav.QuickScanNavHost
import com.quickscan.nav.Routes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Null until the stored preference has actually been read, so the
            // graph is built once with the right start destination rather than
            // being rebuilt when the value arrives.
            val onboarded by settingsRepository.onboardingComplete.collectAsStateWithLifecycle(
                initialValue = null,
            )
            val themeState by settingsRepository.themeState.collectAsStateWithLifecycle(
                initialValue = ThemeState(),
            )

            QuickScanTheme(
                darkTheme = themeState.darkMode,
                accent = themeState.accent,
                largerText = themeState.largerText,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    onboarded?.let { complete ->
                        QuickScanNavHost(
                            startDestination = if (complete) Routes.SCAN else Routes.WELCOME,
                        )
                    }
                }
            }
        }
    }
}