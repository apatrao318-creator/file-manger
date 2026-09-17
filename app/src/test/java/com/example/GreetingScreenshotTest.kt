package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.FileCategory
import com.example.data.model.StorageInfo
import com.example.presentation.components.StorageBar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun app_component_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        StorageBar(
          storageInfo = StorageInfo(
            totalBytes = 64L * 1024 * 1024 * 1024,
            usedBytes = 24L * 1024 * 1024 * 1024,
            freeBytes = 40L * 1024 * 1024 * 1024
          ),
          categoryStats = emptyMap()
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/storage_bar.png")
  }
}
