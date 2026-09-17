package com.example.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.model.ApkInfo
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        val df = DecimalFormat("#,##0.#")
        return "${df.format(value)} ${units[index]}"
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"
        val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (!fromMap.isNullOrEmpty()) return fromMap

        return when (ext) {
            "apk" -> "application/vnd.android.package-archive"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "txt", "log", "md" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "*/*"
        }
    }

    fun isValidFileName(name: String): Boolean {
        if (name.isBlank() || name.length > 255) return false
        val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')
        return name.none { it in invalidChars }
    }

    fun getUniqueDestination(destDir: File, originalName: String): File {
        var target = File(destDir, originalName)
        if (!target.exists()) return target

        val nameWithoutExt = if (originalName.contains(".")) originalName.substringBeforeLast(".") else originalName
        val ext = if (originalName.contains(".")) ".${originalName.substringAfterLast(".")}" else ""

        var counter = 1
        while (target.exists()) {
            target = File(destDir, "$nameWithoutExt ($counter)$ext")
            counter++
        }
        return target
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun openFile(context: Context, file: File): Result<Unit> {
        return try {
            val uri = getUriForFile(context, file)
            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(Exception("No application found to open this file type (${file.extension.uppercase()})"))
        } catch (e: Exception) {
            Result.failure(Exception("Unable to open file: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    fun shareFile(context: Context, file: File): Result<Unit> {
        return try {
            val uri = getUriForFile(context, file)
            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to share file: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    fun shareMultipleFiles(context: Context, files: List<File>): Result<Unit> {
        if (files.isEmpty()) return Result.success(Unit)
        if (files.size == 1) return shareFile(context, files.first())

        return try {
            val uris = ArrayList<Uri>()
            for (f in files) {
                if (f.isFile) {
                    uris.add(getUriForFile(context, f))
                }
            }
            if (uris.isEmpty()) {
                return Result.failure(Exception("No files available to share"))
            }

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share ${files.size} files").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to share files: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    fun getApkInfo(context: Context, file: File): ApkInfo? {
        if (!file.exists() || !file.name.endsWith(".apk", ignoreCase = true)) return null
        return try {
            val pm = context.packageManager
            val flags = PackageManager.GET_META_DATA
            val packageInfo: PackageInfo? = pm.getPackageArchiveInfo(file.absolutePath, flags)
            if (packageInfo != null) {
                val appInfo = packageInfo.applicationInfo
                appInfo?.sourceDir = file.absolutePath
                appInfo?.publicSourceDir = file.absolutePath
                val appName = appInfo?.loadLabel(pm)?.toString() ?: file.nameWithoutExtension
                val icon = appInfo?.loadIcon(pm)
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo?.minSdkVersion ?: 21
                } else 21
                val targetSdk = appInfo?.targetSdkVersion ?: 21

                ApkInfo(
                    appName = appName,
                    packageName = packageInfo.packageName,
                    versionName = packageInfo.versionName ?: "1.0",
                    versionCode = versionCode,
                    minSdk = minSdk,
                    targetSdk = targetSdk,
                    icon = icon,
                    sizeBytes = file.length(),
                    filePath = file.absolutePath
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun installApk(context: Context, file: File): Result<Unit> {
        return try {
            val uri = getUriForFile(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Cannot launch installer: ${e.localizedMessage}"))
        }
    }
}
