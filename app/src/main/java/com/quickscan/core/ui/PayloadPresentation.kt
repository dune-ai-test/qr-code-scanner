package com.quickscan.core.ui

import androidx.annotation.StringRes

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quickscan.R
import com.quickscan.core.ui.component.LucideLink
import com.quickscan.core.ui.component.LucideMail
import com.quickscan.core.ui.component.LucideMapPin
import com.quickscan.core.ui.component.LucideMessageSquare
import com.quickscan.core.ui.component.LucidePhone
import com.quickscan.core.ui.component.LucideType
import com.quickscan.core.ui.component.LucideUser
import com.quickscan.core.ui.component.LucideWifi
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.data.barcode.PayloadType
import com.quickscan.data.barcode.ScannedPayload
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@get:StringRes
val PayloadType.labelRes: Int
    get() = when (this) {
        PayloadType.Url -> R.string.type_website
        PayloadType.Wifi -> R.string.type_wifi
        PayloadType.Text -> R.string.type_text
        PayloadType.Contact -> R.string.type_contact
        PayloadType.Product -> R.string.type_product
        PayloadType.Location -> R.string.type_location
        PayloadType.Phone -> R.string.type_phone
        PayloadType.Sms -> R.string.type_sms
        PayloadType.Email -> R.string.type_email
    }

val PayloadType.icon
    get() = when (this) {
        PayloadType.Url -> LucideLink
        PayloadType.Wifi -> LucideWifi
        PayloadType.Text -> LucideType
        PayloadType.Contact -> LucideUser
        PayloadType.Product -> LucideType
        PayloadType.Location -> LucideMapPin
        PayloadType.Phone -> LucidePhone
        PayloadType.Sms -> LucideMessageSquare
        PayloadType.Email -> LucideMail
    }

/**
 * Row tiles are tinted per type: the accent tint for links, networks and
 * contacts, the warm surface for plain text, matching the History mockup.
 * Plain text keeps the warm tone so it is distinguishable at a glance.
 */
@Composable
fun payloadTileColors(type: PayloadType): Pair<Color, Color> {
    val palette = QsTheme.palette
    return when (type) {
        PayloadType.Text, PayloadType.Product -> palette.warnSurface to palette.warnIcon
        else -> palette.accentTint to palette.accentTintInk
    }
}

/** The headline a payload's own row shows, e.g. the host or the SSID. */
val ScannedPayload.headline: String
    get() = when (this) {
        is ScannedPayload.Url -> url
        is ScannedPayload.Wifi -> ssid
        is ScannedPayload.Contact -> name.ifBlank { email ?: phone ?: "Contact" }
        is ScannedPayload.Product -> value
        is ScannedPayload.Text -> raw
        is ScannedPayload.Location -> label ?: coordinates
        is ScannedPayload.Phone -> number
        is ScannedPayload.Sms -> number
        is ScannedPayload.Email -> address
    }

/**
 * A compact identity for a toast. Unlike [headline] this has to survive a
 * toast's width, so a link is its host rather than the whole URL and plain
 * text is a length rather than a wall of characters.
 */
val ScannedPayload.toastLabel: String
    get() = when (this) {
        is ScannedPayload.Url -> host
        is ScannedPayload.Wifi -> ssid
        is ScannedPayload.Contact -> name.ifBlank { email ?: phone ?: "" }
        is ScannedPayload.Product -> value
        is ScannedPayload.Text -> "${raw.length} characters"
        is ScannedPayload.Location -> label ?: coordinates
        is ScannedPayload.Phone -> number
        is ScannedPayload.Sms -> number
        is ScannedPayload.Email -> address
    }

/**
 * Date formatting for History. Grouping headers are all-caps; row subtitles
 * carry the clock time.
 */
object ScanDates {

    private fun formatter(pattern: String) =
        SimpleDateFormat(pattern, Locale.getDefault())

    fun groupHeader(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val calendar = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val daysBetween = daysBetween(now, timestamp)

        return when {
            isSameDay(target, calendar) -> "TODAY"
            daysBetween == 1L -> "YESTERDAY"
            daysBetween < 7L -> formatter("EEEE").format(Date(timestamp)).uppercase(Locale.getDefault())
            target.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) ->
                formatter("d MMMM").format(Date(timestamp)).uppercase(Locale.getDefault())

            else -> formatter("d MMMM yyyy").format(Date(timestamp)).uppercase(Locale.getDefault())
        }
    }

    fun rowTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val daysBetween = daysBetween(now, timestamp)
        return if (daysBetween < 1L) {
            formatter("h:mm a").format(Date(timestamp))
        } else {
            formatter("d MMM").format(Date(timestamp))
        }
    }

    fun detailTime(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        // Lower-case: this is read mid-sentence, e.g. "Scanned today, 9:41 AM".
        val prefix = when {
            isSameDay(calendar, today) -> "today"
            else -> formatter("d MMM").format(Date(timestamp))
        }
        return "$prefix, ${formatter("h:mm a").format(Date(timestamp))}"
    }

    fun relative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val minutes = (now - timestamp) / 60_000
        return when {
            minutes < 1L -> "just now"
            minutes < 60L -> "$minutes min ago"
            minutes < 60L * 24 -> "${minutes / 60} h ago"
            daysBetween(now, timestamp) == 1L -> "yesterday"
            else -> "${daysBetween(now, timestamp)} days ago"
        }
    }

    private fun daysBetween(now: Long, then: Long): Long {
        val startOfToday = startOfDay(now)
        val startOfThat = startOfDay(then)
        return ((startOfToday - startOfThat) / DAY_MILLIS)
    }

    private fun startOfDay(timestamp: Long) = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun isSameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}