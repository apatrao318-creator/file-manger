package com.example.data.model

import android.graphics.drawable.Drawable
import java.io.File

data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val icon: Drawable? = null,
    val sizeBytes: Long = 0L,
    val filePath: String = ""
) {
    val file: File get() = File(filePath)
}
