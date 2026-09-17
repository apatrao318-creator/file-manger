package com.example.data.model

data class ZipEntryInfo(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val time: Long
)
