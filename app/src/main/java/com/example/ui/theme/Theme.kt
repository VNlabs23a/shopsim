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
    primary = EmeraldMint,
    secondary = NeonCyan,
    tertiary = ElectricBlue,
    background = CyberJet,
    surface = CarbonGrey,
    onPrimary = Color(0xFF003816),
    onSecondary = Color(0xFF00363D),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = DarkCoral
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldMint,
    secondary = NeonCyan,
    tertiary = ElectricBlue,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    error = Color(0xFFD32F2F)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // default to our gorgeous dark theme
  dynamicColor: Boolean = false, // enforce our custom theme colors rather than dynamic system overrides
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
