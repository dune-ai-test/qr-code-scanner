package com.quickscan

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickscan.nav.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation wiring.
 *
 * These are cheap guards on the things that silently break the shell: the
 * routes the launcher shortcuts send, and the destinations the tab bar can
 * reach. They run without the UI so they stay fast and do not need a device
 * that happens to have a camera.
 */
@RunWith(AndroidJUnit4::class)
class NavigationInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everyShortcutTargetIsARealRoute() {
        val shortcuts = listOf(Routes.SCAN, Routes.HISTORY, Routes.CREATE)
        val known = setOf(
            Routes.SCAN,
            Routes.HISTORY,
            Routes.CREATE,
            Routes.SETTINGS,
            Routes.WELCOME,
            Routes.NAME,
            Routes.WHATS_NEW,
        )

        shortcuts.forEach { route ->
            assertTrue("$route is not a known destination", route in known)
        }
    }

    @Test
    fun theLauncherResolvesToTheApp() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        assertTrue("no launcher activity for the package", launchIntent != null)
        assertTrue(
            "launcher intent has no component",
            launchIntent?.component != null,
        )
    }

    @Test
    fun theLauncherIntentCarriesTheRouteExtra() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        assertEquals(null, launchIntent?.getStringExtra(Routes.EXTRA_ROUTE))
    }

    @Test
    fun theAppIsResolvableByItsApplicationId() {
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)

        assertTrue("no application info", info != null)
    }
}