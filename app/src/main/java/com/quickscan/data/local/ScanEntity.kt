package com.quickscan.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One saved scan. Nothing here is ever synchronised off the device. */
@Entity(
    tableName = "scans",
    indices = [Index("createdAt"), Index("type")],
)
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "rawValue")
    val rawValue: String,

    /** [com.quickscan.data.barcode.PayloadType] name. */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "subtitle")
    val subtitle: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    /** [com.quickscan.data.repository.ScanSource] name. */
    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "isPinned")
    val isPinned: Boolean = false,
)