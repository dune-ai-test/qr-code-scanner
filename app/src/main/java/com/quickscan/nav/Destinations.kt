package com.quickscan.nav

import com.quickscan.core.ui.component.TabDestination

object Routes {
    const val WELCOME = "welcome"
    const val NAME = "name"

    const val SCAN = "scan"
    const val HISTORY = "history"
    const val CREATE = "create"
    const val SETTINGS = "settings"
    const val WHATS_NEW = "whats-new"

    const val RESULT = "result/{scanId}"
    fun result(scanId: Long) = "result/$scanId"
}

/** The four capsule-tab destinations, each keeping its own back stack entry. */
fun TabDestination.route(): String = when (this) {
    TabDestination.Scan -> Routes.SCAN
    TabDestination.History -> Routes.HISTORY
    TabDestination.Create -> Routes.CREATE
    TabDestination.Settings -> Routes.SETTINGS
}