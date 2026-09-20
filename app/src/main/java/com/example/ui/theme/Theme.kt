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
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
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
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DokkaniPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E7DD),
    onPrimaryContainer = Color(0xFF0F5132),
    secondary = DokkaniSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = DokkaniTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = DokkaniBackground,
    onBackground = Color(0xFF0F172A),
    surface = DokkaniSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF8FAFC),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
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

@Composable
fun DokkaniTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
