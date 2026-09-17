package com.example.data.model

data class StorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val freePercentage: Float
        get() = if (totalBytes > 0) (freeBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class CategoryStat(
    val category: FileCategory,
    val count: Int = 0,
    val totalSizeBytes: Long = 0L
)
