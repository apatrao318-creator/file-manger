package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CategoryApk
import com.example.ui.theme.CategoryArchives
import com.example.ui.theme.CategoryAudio
import com.example.ui.theme.CategoryDocuments
import com.example.ui.theme.CategoryDownloads
import com.example.ui.theme.CategoryImages
import com.example.ui.theme.CategoryOther
import com.example.ui.theme.CategoryVideos

enum class FileCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val extensions: Set<String>
) {
    IMAGES(
        title = "Images",
        icon = Icons.Default.Image,
        color = CategoryImages,
        extensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg")
    ),
    VIDEOS(
        title = "Videos",
        icon = Icons.Default.VideoLibrary,
        color = CategoryVideos,
        extensions = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "ts", "flv")
    ),
    AUDIO(
        title = "Audio",
        icon = Icons.Default.Audiotrack,
        color = CategoryAudio,
        extensions = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus", "wma")
    ),
    DOCUMENTS(
        title = "Documents",
        icon = Icons.Default.Description,
        color = CategoryDocuments,
        extensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "json", "xml", "log", "rtf", "odt")
    ),
    DOWNLOADS(
        title = "Downloads",
        icon = Icons.Default.Download,
        color = CategoryDownloads,
        extensions = emptySet() // Dynamically mapped to download folder
    ),
    ARCHIVES(
        title = "Archives",
        icon = Icons.Default.FolderZip,
        color = CategoryArchives,
        extensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
    ),
    APK(
        title = "APK Files",
        icon = Icons.Default.Android,
        color = CategoryApk,
        extensions = setOf("apk", "xapk", "apks")
    ),
    OTHER(
        title = "Other Files",
        icon = Icons.Default.InsertDriveFile,
        color = CategoryOther,
        extensions = emptySet()
    );

    companion object {
        fun fromExtension(ext: String): FileCategory {
            val lower = ext.lowercase()
            return values().firstOrNull { it.extensions.contains(lower) } ?: OTHER
        }
    }
}
