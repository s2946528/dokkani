package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SlateElectricBlue,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = SlateTextPrimary,
    secondary = SlateCyan,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = SlateTextPrimary,
    tertiary = SlateVibrantBlue,
    onTertiary = SlateTextPrimary,
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    surfaceContainer = SlateSurface,
    surfaceContainerHigh = SlateSurfaceHigh,
    outline = SlateBorder,
    outlineVariant = SlateBorderLight,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DokkaniPrimary,
    secondary = DokkaniSecondary,
    tertiary = DokkaniTertiary,
    background = DokkaniBackground,
    surface = DokkaniSurface,
    primaryContainer = Color(0xFFD1E7DD),
    onPrimaryContainer = Color(0xFF0F5132)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
