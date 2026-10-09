package com.quickscan.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quickscan.core.ui.component.TabDestination
import com.quickscan.feature.create.CreateScreen
import com.quickscan.feature.history.HistoryScreen
import com.quickscan.feature.onboarding.OnboardingNameScreen
import com.quickscan.feature.onboarding.OnboardingWelcomeScreen
import com.quickscan.feature.result.ResultScreen
import com.quickscan.feature.scanner.ScannerScreen
import com.quickscan.feature.settings.SettingsScreen
import com.quickscan.feature.whatsnew.WhatsNewScreen

@Composable
fun QuickScanNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // One tab is always highlighted; the capsule lives inside each screen, so
    // the host just maps the active route back to its destination.
    val activeTab = remember(currentRoute) {
        TabDestination.entries.firstOrNull { it.route() == currentRoute } ?: TabDestination.Scan
    }

    fun selectTab(destination: TabDestination) {
        val route = destination.route()
        if (route == currentRoute) return
        navController.navigate(route) {
            popUpTo(Routes.SCAN) { inclusive = false; saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.WELCOME) {
            OnboardingWelcomeScreen(
                onGetStarted = {
                    navController.navigate(Routes.NAME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.NAME) {
            OnboardingNameScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Routes.SCAN) {
                        popUpTo(Routes.NAME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SCAN) {
            ScannerScreen(
                onOpenResult = { navController.navigate(Routes.result(it)) },
                onOpenHistory = { selectTab(TabDestination.History) },
                onTabSelected = ::selectTab,
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenScan = { navController.navigate(Routes.result(it)) },
                onStartScanning = { selectTab(TabDestination.Scan) },
                onTabSelected = ::selectTab,
            )
        }

        composable(Routes.CREATE) {
            CreateScreen(
                onBack = { navController.popBackStack() },
                onTabSelected = ::selectTab,
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenWhatsNew = { navController.navigate(Routes.WHATS_NEW) },
                onTabSelected = ::selectTab,
            )
        }

        composable(Routes.WHATS_NEW) {
            WhatsNewScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument("scanId") { type = NavType.LongType }),
        ) {
            ResultScreen(
                onBack = { navController.popBackStack() },
                onTabSelected = ::selectTab,
            )
        }
    }
}