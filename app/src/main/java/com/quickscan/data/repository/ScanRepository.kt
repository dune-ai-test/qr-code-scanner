package com.quickscan.data.repository

import com.quickscan.data.barcode.PayloadParser
import com.quickscan.data.barcode.PayloadType
import com.quickscan.data.barcode.ScannedPayload
import com.quickscan.data.local.ScanDao
import com.quickscan.data.local.ScanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class ScanSource { Camera, Image, Manual }

enum class ScanFilter {
    All,
    Links,
    Wifi,
    Text;

    fun matches(type: PayloadType): Boolean = when (this) {
        All -> true
        Links -> type == PayloadType.Url
        Wifi -> type == PayloadType.Wifi
        Text -> type == PayloadType.Text
    }

}

data class ScanStats(
    val total: Int = 0,
    val thisWeek: Int = 0,
    val links: Int = 0,
)

/**
 * Owns the scan history. Every read and write stays on the device; the only
 * thing that leaves is whatever the user explicitly shares.
 */
class ScanRepository(
    private val dao: ScanDao,
    private val retentionDays: Flow<Int>,
    private val now: () -> Long = System::currentTimeMillis,
) {

    fun observeScans(): Flow<List<ScanEntity>> = dao.observeAll()

    fun observeScan(id: Long): Flow<ScanEntity?> = dao.observeById(id)

    fun observeStats(): Flow<ScanStats> = combine(
        dao.observeTotalCount(),
        dao.observeCountSince(startOfWeekMillis()),
        dao.observeCountOfTypes(listOf(PayloadType.Url.name, PayloadType.Contact.name)),
    ) { total, week, links ->
        ScanStats(total = total, thisWeek = week, links = links)
    }

    /**
     * Saves a scan unless the same payload was recorded moments ago, which is
     * what a code sitting in the camera frame would otherwise do dozens of
     * times per second. Returns the row id either way.
     */
    suspend fun record(
        payload: ScannedPayload,
        source: ScanSource,
        at: Long = now(),
    ): Long {
        dao.findRecent(payload.raw, at - DUPLICATE_WINDOW_MILLIS)?.let { return it.id }
        return dao.insert(
            ScanEntity(
                rawValue = payload.raw,
                type = payload.type.name,
                title = payload.displayTitle(),
                subtitle = payload.displaySubtitle(),
                createdAt = at,
                source = source.name,
            ),
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun deleteAll(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.deleteByIds(ids)
    }

    /** Raw values for a selection, in the order they were scanned. */
    suspend fun rawValues(ids: List<Long>): List<String> =
        dao.findByIds(ids).sortedBy { it.createdAt }.map { it.rawValue }

    /** Pins or unpins a scan, whichever it currently is not. */
    suspend fun togglePinned(id: Long): Boolean {
        val row = dao.findById(id) ?: return false
        val next = !row.isPinned
        dao.update(row.copy(isPinned = next))
        return next
    }

    suspend fun find(id: Long): ScanEntity? = dao.findById(id)

    suspend fun parse(entity: ScanEntity): ScannedPayload =
        PayloadParser.parse(entity.rawValue, entity.type)

    suspend fun clear() = dao.deleteAll()

    /** Drops anything older than the configured retention window. */
    suspend fun pruneExpired(): Int {
        val days = retentionDays.first()
        if (days <= 0) return 0
        dao.deleteOlderThan(now() - TimeUnit.DAYS.toMillis(days.toLong()))
        return 1
    }

    /** Rough on-device footprint, shown on the Settings storage row. */
    suspend fun storageUsage(): StorageUsage {
        val values = dao.allRawValues()
        val bytes = values.sumOf { it.toByteArray().size.toLong() } +
            values.size * ROW_OVERHEAD_BYTES
        return StorageUsage(bytes = bytes, scanCount = values.size)
    }

    private fun ScannedPayload.displayTitle(): String = when (this) {
        is ScannedPayload.Url -> host
        is ScannedPayload.Wifi -> ssid
        is ScannedPayload.Contact -> name.ifBlank { email ?: phone ?: "Contact" }
        is ScannedPayload.Product -> value
        is ScannedPayload.Text -> raw.lineSequence().first().take(64).ifBlank { "Text" }
    }

    private fun ScannedPayload.displaySubtitle(): String = when (this) {
        is ScannedPayload.Url -> url
        is ScannedPayload.Wifi -> displayMeta
        is ScannedPayload.Contact -> listOfNotNull(phone, email).joinToString(" · ")
        is ScannedPayload.Product -> symbology
        is ScannedPayload.Text -> "${raw.length} characters"
    }

    /** Monday 00:00 local time, matching how people read "this week". */
    private fun startOfWeekMillis(): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    companion object {
        private const val DUPLICATE_WINDOW_MILLIS = 3_000L
        private const val ROW_OVERHEAD_BYTES = 192L
    }
}

data class StorageUsage(
    val bytes: Long,
    val scanCount: Int,
)