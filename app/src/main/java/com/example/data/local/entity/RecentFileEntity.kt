package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val path: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val isDirectory: Boolean,
    val lastOpenedTimestamp: Long = System.currentTimeMillis()
)
