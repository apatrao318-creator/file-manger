package com.example.data.model

import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val isHidden: Boolean = file.name.startsWith("."),
    val isFavorite: Boolean = false,
    val itemCount: Int = 0, // for directories, child item count if calculated
    val mimeType: String = ""
) {
    val extension: String
        get() = if (isDirectory) "" else file.extension.lowercase()

    val category: FileCategory
        get() = if (isDirectory) FileCategory.OTHER else FileCategory.fromExtension(extension)

    val isImage: Boolean
        get() = !isDirectory && FileCategory.IMAGES.extensions.contains(extension)

    val isVideo: Boolean
        get() = !isDirectory && FileCategory.VIDEOS.extensions.contains(extension)

    val isAudio: Boolean
        get() = !isDirectory && FileCategory.AUDIO.extensions.contains(extension)

    val isDocument: Boolean
        get() = !isDirectory && FileCategory.DOCUMENTS.extensions.contains(extension)

    val isArchive: Boolean
        get() = !isDirectory && (extension == "zip" || FileCategory.ARCHIVES.extensions.contains(extension))

    val isZip: Boolean
        get() = !isDirectory && extension == "zip"

    val isApk: Boolean
        get() = !isDirectory && extension == "apk"

    val isText: Boolean
        get() = !isDirectory && setOf("txt", "csv", "json", "xml", "log", "md", "html", "htm", "css", "js", "kt", "java", "py", "sh").contains(extension)
}
