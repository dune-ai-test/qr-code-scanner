package com.quickscan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scans ORDER BY isPinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    fun observeById(id: Long): Flow<ScanEntity?>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun findById(id: Long): ScanEntity?

    @Query(
        "SELECT * FROM scans WHERE rawValue = :rawValue AND createdAt >= :since " +
            "ORDER BY createdAt DESC LIMIT 1",
    )
    suspend fun findRecent(rawValue: String, since: Long): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity): Long

    @Update
    suspend fun update(scan: ScanEntity)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scans")
    suspend fun deleteAll()

    @Query("DELETE FROM scans WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM scans")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scans WHERE createdAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM scans WHERE type IN (:types)")
    fun observeCountOfTypes(types: List<String>): Flow<Int>

    @Query("SELECT rawValue FROM scans")
    suspend fun allRawValues(): List<String>
}