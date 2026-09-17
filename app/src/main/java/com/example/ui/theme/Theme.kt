package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = BrandOnPrimaryDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    secondary = DeepOceanBlueDark,
    onSecondary = DeepOceanOnSecondaryDark,
    secondaryContainer = DeepOceanSecondaryContainerDark,
    onSecondaryContainer = DeepOceanOnSecondaryContainerDark,
    tertiary = OceanTealDark,
    onTertiary = OceanTealOnDark,
    tertiaryContainer = OceanTealContainerDark,
    onTertiaryContainer = OceanTealOnContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF1E2B3E),
    error = StatusError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = BrandOnPrimaryLight,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = BrandOnPrimaryContainerLight,
    secondary = DeepOceanBlueLight,
    onSecondary = DeepOceanOnSecondaryLight,
    secondaryContainer = DeepOceanSecondaryContainerLight,
    onSecondaryContainer = DeepOceanOnSecondaryContainerLight,
    tertiary = OceanTealLight,
    onTertiary = OceanTealOnLight,
    tertiaryContainer = OceanTealContainerLight,
    onTertiaryContainer = OceanTealOnContainerLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE2E8F0),
    error = StatusError,
    onError = Color.White
)

/**
 * Theme state representation defaulting to Light Mode instead of system theme.
 */
enum class ThemeState(val isDark: Boolean) {
    LIGHT(false),
    DARK(true),
    SYSTEM(false);

    companion object {
        val DEFAULT: ThemeState = LIGHT
    }
}

data class ThemeConfig(
    val themeState: ThemeState = ThemeState.LIGHT
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Defaults to Light Mode instead of system theme
    dynamicColor: Boolean = false, // Keep intentional brand identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
