package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalPath: String,
    val trashPath: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val deletedTimestamp: Long = System.currentTimeMillis()
)
