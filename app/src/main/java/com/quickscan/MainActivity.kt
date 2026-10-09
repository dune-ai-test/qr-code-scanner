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

private val ROUTE_SHORTCUTS = setOf(Routes.SCAN, Routes.HISTORY, Routes.CREATE)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var requestedRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A launcher shortcut names the screen it wants; anything else, or a
        // repeat tap on the icon while already running, falls through to the
        // normal entry point.
        requestedRoute = intent?.getStringExtra(Routes.EXTRA_ROUTE)
            ?.takeIf { it in ROUTE_SHORTCUTS }

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
                            startDestination = when {
                                !complete -> Routes.WELCOME
                                requestedRoute != null -> requestedRoute
                                else -> Routes.SCAN
                            },
                        )
                    }
                }
            }
        }
    }
}