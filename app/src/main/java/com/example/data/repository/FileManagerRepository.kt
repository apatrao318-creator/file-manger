package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.RecentFileEntity
import com.example.data.local.entity.TrashItemEntity
import com.example.data.model.FileItem
import com.example.data.model.SortOrder
import com.example.data.model.SortType
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileManagerRepository(
    private val database: AppDatabase
) {
    private val recentFileDao = database.recentFileDao()
    private val favoriteDao = database.favoriteDao()
    private val trashDao = database.trashDao()

    val recentFiles: Flow<List<RecentFileEntity>> = recentFileDao.getRecentFiles()
    val favorites: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
    val trashItems: Flow<List<TrashItemEntity>> = trashDao.getAllTrash()

    fun isFavorite(path: String): Flow<Boolean> = favoriteDao.isFavorite(path)

    suspend fun addFavorite(file: File) = withContext(Dispatchers.IO) {
        val entity = FavoriteEntity(
            path = file.absolutePath,
            name = file.name,
            isDirectory = file.isDirectory,
            size = if (file.isDirectory) 0L else file.length(),
            mimeType = FileUtils.getMimeType(file)
        )
        favoriteDao.insertFavorite(entity)
    }

    suspend fun removeFavorite(path: String) = withContext(Dispatchers.IO) {
        favoriteDao.deleteFavorite(path)
    }

    suspend fun recordRecentFile(file: File) = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext
        val entity = RecentFileEntity(
            path = file.absolutePath,
            name = file.name,
            size = if (file.isDirectory) 0L else file.length(),
            mimeType = FileUtils.getMimeType(file),
            isDirectory = file.isDirectory,
            lastOpenedTimestamp = System.currentTimeMillis()
        )
        recentFileDao.insertRecentFile(entity)
    }

    suspend fun removeRecentFile(path: String) = withContext(Dispatchers.IO) {
        recentFileDao.deleteByPath(path)
    }

    suspend fun clearRecentFiles() = withContext(Dispatchers.IO) {
        recentFileDao.clearAll()
    }

    suspend fun listFiles(
        directory: File,
        showHidden: Boolean,
        sortType: SortType,
        sortOrder: SortOrder
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext emptyList()
        }

        val rawFiles = directory.listFiles() ?: return@withContext emptyList()

        val items = rawFiles
            .filter { file ->
                if (!showHidden && file.name.startsWith(".")) false else true
            }
            .map { file ->
                val count = if (file.isDirectory) {
                    try { file.list()?.size ?: 0 } catch (e: Exception) { 0 }
                } else 0
                val isFav = favoriteDao.isFavoriteSync(file.absolutePath)
                FileItem(
                    file = file,
                    itemCount = count,
                    isFavorite = isFav,
                    mimeType = FileUtils.getMimeType(file)
                )
            }

        // Sort: Folders first always, then by requested sortType & sortOrder
        val comparator = Comparator<FileItem> { a, b ->
            if (a.isDirectory && !b.isDirectory) return@Comparator -1
            if (!a.isDirectory && b.isDirectory) return@Comparator 1

            val comp = when (sortType) {
                SortType.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                SortType.SIZE -> a.size.compareTo(b.size)
                SortType.DATE -> a.lastModified.compareTo(b.lastModified)
                SortType.TYPE -> a.extension.compareTo(b.extension, ignoreCase = true)
            }
            if (sortOrder == SortOrder.ASCENDING) comp else -comp
        }

        items.sortedWith(comparator)
    }

    suspend fun searchFiles(
        rootDir: File,
        query: String,
        extensionFilter: String? = null,
        maxResults: Int = 150
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        val trimmed = query.trim().lowercase()

        fun searchRecursive(dir: File, depth: Int) {
            if (depth > 6 || results.size >= maxResults) return
            val files = dir.listFiles() ?: return

            for (f in files) {
                if (results.size >= maxResults) return
                if (f.name.startsWith(".") || f.name.equals("data", true) || f.name.equals("obb", true)) {
                    continue
                }

                val nameMatches = trimmed.isEmpty() || f.name.lowercase().contains(trimmed)
                val extMatches = extensionFilter.isNullOrEmpty() || f.extension.equals(extensionFilter, ignoreCase = true)

                if (nameMatches && extMatches) {
                    results.add(FileItem(file = f))
                }

                if (f.isDirectory) {
                    searchRecursive(f, depth + 1)
                }
            }
        }

        try {
            if (rootDir.exists() && rootDir.canRead()) {
                searchRecursive(rootDir, 0)
            }
        } catch (ignored: Exception) {
        }

        results.sortedByDescending { it.lastModified }
    }

    suspend fun createFolder(parent: File, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        if (!FileUtils.isValidFileName(folderName)) {
            return@withContext Result.failure(Exception("Invalid folder name"))
        }
        val target = File(parent, folderName)
        if (target.exists()) {
            return@withContext Result.failure(Exception("Folder already exists"))
        }
        val created = target.mkdirs()
        if (created) Result.success(target) else Result.failure(Exception("Failed to create folder"))
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        if (!FileUtils.isValidFileName(newName)) {
            return@withContext Result.failure(Exception("Invalid name"))
        }
        val target = File(file.parentFile, newName)
        if (target.exists()) {
            return@withContext Result.failure(Exception("A file or folder with this name already exists"))
        }
        val renamed = file.renameTo(target)
        if (renamed) {
            // Update in favorite and recent if needed
            favoriteDao.deleteFavorite(file.absolutePath)
            recentFileDao.deleteByPath(file.absolutePath)
            Result.success(target)
        } else {
            Result.failure(Exception("Failed to rename item"))
        }
    }

    suspend fun copyFiles(
        sources: List<File>,
        destDir: File,
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!destDir.exists()) destDir.mkdirs()
        var count = 0
        val total = sources.size

        try {
            for ((index, src) in sources.withIndex()) {
                val destFile = FileUtils.getUniqueDestination(destDir, src.name)
                onProgress(index + 1, total, src.name)
                if (src.isDirectory) {
                    src.copyRecursively(destFile, overwrite = false)
                } else {
                    src.copyTo(destFile, overwrite = false)
                }
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("Copy failed: ${e.localizedMessage}"))
        }
    }

    suspend fun moveFiles(
        sources: List<File>,
        destDir: File,
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!destDir.exists()) destDir.mkdirs()
        var count = 0
        val total = sources.size

        try {
            for ((index, src) in sources.withIndex()) {
                val destFile = FileUtils.getUniqueDestination(destDir, src.name)
                onProgress(index + 1, total, src.name)
                val moved = src.renameTo(destFile)
                if (!moved) {
                    // Fallback to copy + delete
                    if (src.isDirectory) {
                        src.copyRecursively(destFile, overwrite = true)
                        src.deleteRecursively()
                    } else {
                        src.copyTo(destFile, overwrite = true)
                        src.delete()
                    }
                }
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("Move failed: ${e.localizedMessage}"))
        }
    }

    suspend fun deletePermanently(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        try {
            for (f in files) {
                val deleted = if (f.isDirectory) f.deleteRecursively() else f.delete()
                if (deleted) {
                    count++
                    favoriteDao.deleteFavorite(f.absolutePath)
                    recentFileDao.deleteByPath(f.absolutePath)
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("Delete failed: ${e.localizedMessage}"))
        }
    }

    suspend fun moveToTrash(context: Context, file: File): Result<TrashItemEntity> = withContext(Dispatchers.IO) {
        try {
            val trashDir = File(context.getExternalFilesDir(null) ?: context.filesDir, ".trash")
            if (!trashDir.exists()) trashDir.mkdirs()

            val trashTarget = File(trashDir, "${System.currentTimeMillis()}_${file.name}")
            val moved = file.renameTo(trashTarget)
            val finalTrashFile = if (moved) trashTarget else {
                if (file.isDirectory) {
                    file.copyRecursively(trashTarget)
                    file.deleteRecursively()
                } else {
                    file.copyTo(trashTarget)
                    file.delete()
                }
                trashTarget
            }

            val entity = TrashItemEntity(
                originalPath = file.absolutePath,
                trashPath = finalTrashFile.absolutePath,
                name = file.name,
                size = if (file.isDirectory) 0L else finalTrashFile.length(),
                isDirectory = file.isDirectory
            )
            val id = trashDao.insertTrash(entity)
            favoriteDao.deleteFavorite(file.absolutePath)
            recentFileDao.deleteByPath(file.absolutePath)
            Result.success(entity.copy(id = id.toInt()))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to move to trash: ${e.localizedMessage}"))
        }
    }

    suspend fun restoreTrashItem(item: TrashItemEntity): Result<File> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            if (!trashFile.exists()) {
                trashDao.deleteTrashById(item.id)
                return@withContext Result.failure(Exception("Trash item file no longer exists"))
            }

            val origFile = File(item.originalPath)
            val targetDir = origFile.parentFile ?: File(origFile.path)
            if (!targetDir.exists()) targetDir.mkdirs()

            val restoreTarget = FileUtils.getUniqueDestination(targetDir, origFile.name)
            val restored = trashFile.renameTo(restoreTarget)
            if (!restored) {
                if (trashFile.isDirectory) {
                    trashFile.copyRecursively(restoreTarget)
                    trashFile.deleteRecursively()
                } else {
                    trashFile.copyTo(restoreTarget)
                    trashFile.delete()
                }
            }

            trashDao.deleteTrashById(item.id)
            Result.success(restoreTarget)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to restore item: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteTrashPermanently(item: TrashItemEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            if (trashFile.exists()) {
                if (trashFile.isDirectory) trashFile.deleteRecursively() else trashFile.delete()
            }
            trashDao.deleteTrashById(item.id)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to permanently delete: ${e.localizedMessage}"))
        }
    }

    suspend fun emptyTrash(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            trashDao.clearAll()
            Result.success(1)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to empty trash: ${e.localizedMessage}"))
        }
    }
}
