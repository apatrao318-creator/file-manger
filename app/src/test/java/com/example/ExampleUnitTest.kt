package com.example

import com.example.data.model.FileCategory
import com.example.data.model.SortOrder
import com.example.data.model.SortType
import com.example.utils.FileUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun formatFileSize_correctUnits() {
    assertEquals("0 B", FileUtils.formatFileSize(0L))
    assertEquals("500 B", FileUtils.formatFileSize(500L))
    assertEquals("1 KB", FileUtils.formatFileSize(1024L))
    assertEquals("1.5 MB", FileUtils.formatFileSize((1.5 * 1024 * 1024).toLong()))
    assertEquals("2 GB", FileUtils.formatFileSize(2L * 1024 * 1024 * 1024))
  }

  @Test
  fun isValidFileName_validation() {
    assertTrue(FileUtils.isValidFileName("my_document.pdf"))
    assertTrue(FileUtils.isValidFileName("vacation 2026.jpg"))
    assertFalse(FileUtils.isValidFileName(""))
    assertFalse(FileUtils.isValidFileName("file/name.txt"))
    assertFalse(FileUtils.isValidFileName("file\\name.txt"))
    assertFalse(FileUtils.isValidFileName("file:name.txt"))
  }

  @Test
  fun fileCategory_mapping() {
    assertEquals(FileCategory.IMAGES, FileCategory.fromExtension("jpg"))
    assertEquals(FileCategory.IMAGES, FileCategory.fromExtension("PNG"))
    assertEquals(FileCategory.VIDEOS, FileCategory.fromExtension("mp4"))
    assertEquals(FileCategory.AUDIO, FileCategory.fromExtension("mp3"))
    assertEquals(FileCategory.DOCUMENTS, FileCategory.fromExtension("pdf"))
    assertEquals(FileCategory.ARCHIVES, FileCategory.fromExtension("zip"))
    assertEquals(FileCategory.APK, FileCategory.fromExtension("apk"))
    assertEquals(FileCategory.OTHER, FileCategory.fromExtension("xyz123"))
  }
}
