package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = HighDensityBlue,
    secondary = HighDensitySlateGray,
    tertiary = HighDensityCyan,
    background = HighDensityDarkSlate,
    surface = HighDensityDarkSlate,
    surfaceVariant = HighDensitySlateGray,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = HighDensityBg
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HighDensityBlue,
    secondary = HighDensitySlateGray,
    tertiary = HighDensityCyan,
    background = HighDensityBg,
    surface = HighDensityCardBg,
    surfaceVariant = HighDensityBorder,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = HighDensityDarkSlate,
    onSurface = HighDensityDarkSlate,
    onSurfaceVariant = HighDensitySlateLight,
    outline = HighDensityBorder,
    outlineVariant = HighDensityBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the bespoke High Density design aesthetic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
