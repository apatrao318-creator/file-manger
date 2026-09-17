package com.example.utils

import com.example.data.model.ZipEntryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    suspend fun listZipEntries(zipFile: File): List<ZipEntryInfo> = withContext(Dispatchers.IO) {
        val entriesList = mutableListOf<ZipEntryInfo>()
        if (!zipFile.exists() || !zipFile.canRead()) return@withContext entriesList

        try {
            ZipFile(zipFile).use { zf ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    entriesList.add(
                        ZipEntryInfo(
                            name = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size.coerceAtLeast(0L),
                            compressedSize = entry.compressedSize.coerceAtLeast(0L),
                            time = entry.time
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // fallback to ZipInputStream if ZipFile fails
            try {
                ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        entriesList.add(
                            ZipEntryInfo(
                                name = entry.name,
                                isDirectory = entry.isDirectory,
                                size = entry.size.coerceAtLeast(0L),
                                compressedSize = entry.compressedSize.coerceAtLeast(0L),
                                time = entry.time
                            )
                        )
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } catch (ignored: Exception) {
            }
        }
        entriesList
    }

    suspend fun extractZip(
        zipFile: File,
        destDir: File,
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!zipFile.exists()) {
            return@withContext Result.failure(Exception("Archive file does not exist"))
        }
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        try {
            val totalCount: Int = try {
                ZipFile(zipFile).use { it.size() }
            } catch (e: Exception) {
                0
            }

            var extractedCount = 0
            val destCanonicalPath = destDir.canonicalPath

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                val buffer = ByteArray(8192)
                var entry = zis.nextEntry

                while (entry != null) {
                    val entryName = entry.name
                    val outFile = File(destDir, entryName)

                    // Path traversal (Zip Slip) security protection:
                    val canonicalOut = outFile.canonicalPath
                    if (!canonicalOut.startsWith(destCanonicalPath + File.separator) && canonicalOut != destCanonicalPath) {
                        throw SecurityException("Zip entry is outside of the target dir: $entryName")
                    }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            BufferedOutputStream(fos).use { bos ->
                                var bytesRead: Int
                                while (zis.read(buffer).also { bytesRead = it } != -1) {
                                    bos.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                    }

                    extractedCount++
                    onProgress(extractedCount, if (totalCount > 0) totalCount else extractedCount + 1, entryName)
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(extractedCount)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to extract archive: ${e.localizedMessage ?: "Corrupted file"}"))
        }
    }

    suspend fun createZip(
        sources: List<File>,
        destZip: File,
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (sources.isEmpty()) {
            return@withContext Result.failure(Exception("No files selected to archive"))
        }

        try {
            // count total files
            var totalFiles = 0
            fun countFiles(f: File) {
                if (f.isDirectory) {
                    val children = f.listFiles() ?: emptyArray()
                    if (children.isEmpty()) totalFiles++
                    else children.forEach { countFiles(it) }
                } else {
                    totalFiles++
                }
            }
            sources.forEach { countFiles(it) }
            val total = totalFiles.coerceAtLeast(1)

            destZip.parentFile?.mkdirs()
            var processed = 0

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destZip))).use { zos ->
                val buffer = ByteArray(8192)

                fun addEntry(file: File, basePath: String) {
                    val entryPath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
                    if (file.isDirectory) {
                        val children = file.listFiles() ?: emptyArray()
                        if (children.isEmpty()) {
                            val dirEntry = ZipEntry("$entryPath/")
                            zos.putNextEntry(dirEntry)
                            zos.closeEntry()
                            processed++
                            onProgress(processed, total, file.name)
                        } else {
                            for (child in children) {
                                addEntry(child, entryPath)
                            }
                        }
                    } else {
                        val entry = ZipEntry(entryPath)
                        entry.time = file.lastModified()
                        zos.putNextEntry(entry)

                        FileInputStream(file).use { fis ->
                            BufferedInputStream(fis).use { bis ->
                                var len: Int
                                while (bis.read(buffer).also { len = it } != -1) {
                                    zos.write(buffer, 0, len)
                                }
                            }
                        }
                        zos.closeEntry()
                        processed++
                        onProgress(processed, total, file.name)
                    }
                }

                for (source in sources) {
                    addEntry(source, "")
                }
            }
            Result.success(destZip)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to create ZIP: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }
}
