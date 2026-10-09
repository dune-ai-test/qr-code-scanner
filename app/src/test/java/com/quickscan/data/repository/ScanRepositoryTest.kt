package com.quickscan.data.repository

import com.quickscan.data.barcode.PayloadType
import com.quickscan.data.barcode.ScannedPayload
import com.quickscan.data.local.ScanDao
import com.quickscan.data.local.ScanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory stand-in for the Room DAO. */
private class FakeScanDao : ScanDao {
    val rows = mutableListOf<ScanEntity>()
    private var nextId = 1L

    override fun observeAll(): Flow<List<ScanEntity>> =
        flowOf(rows.sortedByDescending { it.createdAt })

    override fun observeById(id: Long): Flow<ScanEntity?> = flowOf(rows.find { it.id == id })

    override suspend fun findById(id: Long): ScanEntity? = rows.find { it.id == id }

    override suspend fun findRecent(rawValue: String, since: Long): ScanEntity? =
        rows.filter { it.rawValue == rawValue && it.createdAt >= since }
            .maxByOrNull { it.createdAt }

    override suspend fun insert(scan: ScanEntity): Long {
        val id = nextId++
        rows += scan.copy(id = id)
        return id
    }

    override suspend fun update(scan: ScanEntity) {
        val index = rows.indexOfFirst { it.id == scan.id }
        if (index >= 0) rows[index] = scan
    }

    override suspend fun deleteById(id: Long) {
        rows.removeAll { it.id == id }
    }

    override suspend fun deleteAll() {
        rows.clear()
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        rows.removeAll { it.createdAt < cutoff }
    }

    override fun observeTotalCount(): Flow<Int> = flowOf(rows.size)

    override fun observeCountSince(since: Long): Flow<Int> =
        flowOf(rows.count { it.createdAt >= since })

    override fun observeCountOfTypes(types: List<String>): Flow<Int> =
        flowOf(rows.count { it.type in types })

    override suspend fun allRawValues(): List<String> = rows.map { it.rawValue }
}

private const val NOW = 1_000_000_000L

private fun repository(
    dao: FakeScanDao = FakeScanDao(),
    retentionDays: Int = 30,
    now: Long = NOW,
): ScanRepository = ScanRepository(dao, MutableStateFlow(retentionDays), now = { now })

private val url = ScannedPayload.Url(
    raw = "https://example.com",
    url = "https://example.com",
    host = "example.com",
    scheme = "https",
)

private val wifi = ScannedPayload.Wifi(
    raw = "WIFI:T:WPA;S:N;P:p;;",
    ssid = "N",
    password = "p",
    encryption = "WPA",
    hidden = false,
)

class ScanRepositoryTest {

    @Test
    fun `recording a scan stores the parsed title and type`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao)

        val id = repo.record(url, ScanSource.Camera)

        assertEquals(1, dao.rows.size)
        assertEquals("example.com", dao.rows.single().title)
        assertEquals(PayloadType.Url.name, dao.rows.single().type)
        assertEquals(id, dao.rows.single().id)
    }

    @Test
    fun `the same payload inside the dedupe window is not stored twice`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao)

        val first = repo.record(url, ScanSource.Camera, at = 1_000L)
        val second = repo.record(url, ScanSource.Camera, at = 1_500L)

        assertEquals(first, second)
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `the same payload after the dedupe window is stored again`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao)

        repo.record(url, ScanSource.Camera, at = 1_000L)
        repo.record(url, ScanSource.Camera, at = 30_000L)

        assertEquals(2, dao.rows.size)
    }

    @Test
    fun `a different payload is always stored`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao)

        repo.record(url, ScanSource.Camera, at = 1_000L)
        repo.record(ScannedPayload.Text("something else"), ScanSource.Image, at = 1_100L)

        assertEquals(2, dao.rows.size)
    }

    @Test
    fun `deleting removes only the requested row`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao)
        val first = repo.record(url, ScanSource.Camera)
        repo.record(ScannedPayload.Text("other"), ScanSource.Camera)

        repo.delete(first)

        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `clear removes everything`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao)
        repo.record(url, ScanSource.Camera)
        repo.record(ScannedPayload.Text("other"), ScanSource.Camera)

        repo.clear()

        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `pruning drops scans older than the retention window`() = runTest {
        val dao = FakeScanDao()
        val day = 24L * 60 * 60 * 1000
        val now = 400L * day
        val repo = repository(dao, retentionDays = 30, now = now)

        repo.record(url, ScanSource.Camera, at = now - 60L * day)
        repo.record(url, ScanSource.Camera, at = now - day)

        repo.pruneExpired()

        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `pruning is a no-op when retention is forever`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao, retentionDays = 0, now = 400L * 24 * 60 * 60 * 1000)

        repo.record(url, ScanSource.Camera, at = 0L)
        repo.pruneExpired()

        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `storage usage grows with the number of scans`() = runTest {
        val repo = repository()

        repo.record(url, ScanSource.Camera)
        val afterOne = repo.storageUsage()

        repo.record(url, ScanSource.Camera, at = 60_000L)
        val afterTwo = repo.storageUsage()

        assertEquals(1, afterOne.scanCount)
        assertEquals(2, afterTwo.scanCount)
        assertTrue(afterTwo.bytes > afterOne.bytes)
    }

    @Test
    fun `stats count links but not text`() = runTest {
        val repo = repository()
        repo.record(url, ScanSource.Camera)
        repo.record(ScannedPayload.Text("hello"), ScanSource.Camera)

        val stats = repo.observeStats().first()

        assertEquals(2, stats.total)
        assertEquals(1, stats.links)
    }

    @Test
    fun `each filter selects its own payload types`() = runTest {
        val repo = repository()
        repo.record(url, ScanSource.Camera)
        repo.record(wifi, ScanSource.Camera)
        repo.record(ScannedPayload.Text("note"), ScanSource.Camera)

        val types = repo.observeScans().first()
            .map { PayloadType.valueOf(it.type) }

        assertEquals(3, types.count { ScanFilter.All.matches(it) })
        assertEquals(1, types.count { ScanFilter.Links.matches(it) })
        assertEquals(1, types.count { ScanFilter.Wifi.matches(it) })
        assertEquals(1, types.count { ScanFilter.Text.matches(it) })
    }

    @Test
    fun `parsing a stored row reproduces the original payload`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao)
        val id = repo.record(wifi, ScanSource.Camera)

        val parsed = repo.parse(dao.rows.single { it.id == id })

        assertTrue(parsed is ScannedPayload.Wifi)
        assertEquals("N", (parsed as ScannedPayload.Wifi).ssid)
    }

    @Test
    fun `text rows keep a single-line title`() = runTest {
        val dao = FakeScanDao()
        val repo = repository(dao)

        repo.record(ScannedPayload.Text("first line\nsecond line"), ScanSource.Camera)

        val title = dao.rows.single().title
        assertFalse(title.contains('\n'))
    }
}