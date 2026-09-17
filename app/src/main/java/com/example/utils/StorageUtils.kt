package com.example.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.data.model.CategoryStat
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StorageUtils {

    fun getPrimaryStorageRoot(): File {
        val external = Environment.getExternalStorageDirectory()
        return if (external != null && external.exists() && external.canRead()) {
            external
        } else {
            File("/storage/emulated/0")
        }
    }

    fun getStorageInfo(): StorageInfo {
        return try {
            val root = getPrimaryStorageRoot()
            val stat = StatFs(root.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            StorageInfo(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes
            )
        } catch (e: Exception) {
            StorageInfo()
        }
    }

    suspend fun scanCategoryStats(context: Context): Map<FileCategory, CategoryStat> = withContext(Dispatchers.IO) {
        val statsMap = mutableMapOf<FileCategory, MutableList<Long>>()
        FileCategory.values().forEach { statsMap[it] = mutableListOf() }

        val root = getPrimaryStorageRoot()
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val downloadCanonical = try { downloadDir?.canonicalPath } catch (e: Exception) { null }

        // Scan primary storage directories up to depth 5, skipping private Android/data which might be restricted
        fun scanDir(dir: File, depth: Int) {
            if (depth > 5) return
            val files = dir.listFiles() ?: return

            for (file in files) {
                if (file.isDirectory) {
                    // Skip hidden dirs, Android/data or Android/obb system restricted directories
                    if (file.name.startsWith(".") || file.name.equals("data", ignoreCase = true) || file.name.equals("obb", ignoreCase = true)) {
                        continue
                    }
                    scanDir(file, depth + 1)
                } else {
                    val len = file.length()
                    // Check if in Downloads
                    val isDownload = downloadCanonical != null && file.parentFile?.canonicalPath?.startsWith(downloadCanonical) == true
                    if (isDownload) {
                        statsMap[FileCategory.DOWNLOADS]?.add(len)
                    }

                    val cat = FileCategory.fromExtension(file.extension)
                    statsMap[cat]?.add(len)
                }
            }
        }

        try {
            if (root.exists() && root.canRead()) {
                scanDir(root, 0)
            }
        } catch (ignored: Exception) {
        }

        statsMap.mapValues { (cat, sizes) ->
            CategoryStat(
                category = cat,
                count = sizes.size,
                totalSizeBytes = sizes.sum()
            )
        }
    }

    suspend fun getFilesForCategory(category: FileCategory, maxItems: Int = 200): List<FileItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FileItem>()
        val root = getPrimaryStorageRoot()

        if (category == FileCategory.DOWNLOADS) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists()) {
                val files = downloadDir.listFiles() ?: emptyArray()
                for (f in files) {
                    if (f.isFile) {
                        result.add(FileItem(file = f))
                    }
                }
            }
            return@withContext result.sortedByDescending { it.lastModified }
        }

        fun scanForCategory(dir: File, depth: Int) {
            if (depth > 5 || result.size >= maxItems) return
            val files = dir.listFiles() ?: return

            for (f in files) {
                if (result.size >= maxItems) return
                if (f.isDirectory) {
                    if (f.name.startsWith(".") || f.name.equals("data", true) || f.name.equals("obb", true)) continue
                    scanForCategory(f, depth + 1)
                } else {
                    val cat = FileCategory.fromExtension(f.extension)
                    if (cat == category) {
                        result.add(FileItem(file = f))
                    }
                }
            }
        }

        try {
            if (root.exists() && root.canRead()) {
                scanForCategory(root, 0)
            }
        } catch (ignored: Exception) {
        }

        result.sortedByDescending { it.lastModified }
    }

    suspend fun getLargestFiles(limit: Int = 15): List<FileItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FileItem>()
        val root = getPrimaryStorageRoot()

        fun scanLargest(dir: File, depth: Int) {
            if (depth > 5) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isDirectory) {
                    if (f.name.startsWith(".") || f.name.equals("data", true) || f.name.equals("obb", true)) continue
                    scanLargest(f, depth + 1)
                } else {
                    if (f.length() > 5 * 1024 * 1024) { // Only files > 5MB
                        list.add(FileItem(file = f))
                    }
                }
            }
        }

        try {
            if (root.exists() && root.canRead()) {
                scanLargest(root, 0)
            }
        } catch (ignored: Exception) {
        }

        list.sortedByDescending { it.size }.take(limit)
    }
}
