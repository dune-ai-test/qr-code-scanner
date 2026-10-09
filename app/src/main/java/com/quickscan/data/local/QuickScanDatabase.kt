package com.quickscan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScanEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QuickScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        const val NAME = "quickscan.db"
    }
}