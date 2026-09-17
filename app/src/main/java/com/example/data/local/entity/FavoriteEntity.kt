package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val mimeType: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)
