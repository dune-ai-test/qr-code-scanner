package com.quickscan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScanEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class QuickScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        const val NAME = "quickscan.db"

        /**
         * Adds the style column without touching the rows already there.
         *
         * The builder also falls back to a destructive migration, so without
         * this the app would quietly delete every scan the user has made, just
         * to add a nullable column. Existing rows land on null, which reads as
         * "never styled" — the correct answer for everything scanned so far.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scans ADD COLUMN qrStyle TEXT")
            }
        }
    }
}