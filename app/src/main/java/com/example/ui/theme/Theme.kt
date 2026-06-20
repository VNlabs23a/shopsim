package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily

enum class AppTheme {
    CYBERPUNK_DARK,
    M3_EXPRESSIVE,
    OCEAN_BREEZE,
    SUNSET_EMBER
}

// Data class storing layout structure customization parameters per theme
data class AppThemeProperties(
    val cardShape: Shape,
    val buttonShape: Shape,
    val borderWidth: Dp,
    val isBorderGlow: Boolean,
    val headerFontFamily: FontFamily,
    val bodyFontFamily: FontFamily,
    val containerPadding: Dp,
    val cardElevation: Dp,
    val labelWeight: androidx.compose.ui.text.font.FontWeight,
    val isAsymmetricStyle: Boolean = false,
    
    // Radical Placement and Design Layout customization properties
    val navigationLayout: String, // "hardware_dock", "floating_bubble", "minimal_foam", "slanted_asymmetric"
    val showScanlineOverlay: Boolean,
    val useCompactTechSpecs: Boolean, // strict key-value pairs of statistics with grid alignment
    val asymmetricBorderAlternator: Boolean // staggered list rendering shapes
)

// Default Properties (Cyberpunk default style)
val DefaultThemeProperties = AppThemeProperties(
    cardShape = RoundedCornerShape(2.dp),
    buttonShape = RoundedCornerShape(4.dp),
    borderWidth = 1.5.dp,
    isBorderGlow = true,
    headerFontFamily = FontFamily.Monospace,
    bodyFontFamily = FontFamily.Monospace,
    containerPadding = 12.dp,
    cardElevation = 0.dp,
    labelWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
    isAsymmetricStyle = false,
    navigationLayout = "hardware_dock",
    showScanlineOverlay = true,
    useCompactTechSpecs = true,
    asymmetricBorderAlternator = false
)

val LocalAppThemeProperties = staticCompositionLocalOf { DefaultThemeProperties }

// 1. Cyberpunk Theme Colors
private val CyberpunkDarkColorScheme = darkColorScheme(
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

private val CyberpunkLightColorScheme = lightColorScheme(
    primary = EmeraldMint,
    secondary = NeonCyan,
    tertiary = ElectricBlue,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1B2026),
    onSurface = Color(0xFF1B2026),
    error = Color(0xFFD32F2F)
)

// 2. Material 3 Expressive (Energetic magenta, warm custom terracotta, teal contrast)
private val M3ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE040FB), // Magenta energy
    secondary = Color(0xFFFF6D00), // Energetic deep orange
    tertiary = Color(0xFF00E5FF), // Teal spark
    background = Color(0xFF120C1F), // Rich dark plum
    surface = Color(0xFF1D1433), // Warm purple surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF3E8FF),
    onSurface = Color(0xFFF3E8FF),
    error = Color(0xFFFF5252)
)

private val M3ExpressiveLightColorScheme = lightColorScheme(
    primary = Color(0xFFC2185B), // Rich pink/rose magenta
    secondary = Color(0xFFE65100), // Terracotta clay orange
    tertiary = Color(0xFF00838F), // Artistic teal
    background = Color(0xFFFFF9E6), // Cozy warm custard/vanilla
    surface = Color(0xFFFFFDE7), // Soft cream card surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF332F21),
    onSurface = Color(0xFF332F21),
    error = Color(0xFFC62828)
)

// 3. Ocean Breeze Theme Colors
private val OceanBreezeDarkColorScheme = darkColorScheme(
    primary = Color(0xFF26A69A), // Deep vibrant teal
    secondary = Color(0xFF4DB6AC), // Soft mint green
    tertiary = Color(0xFF00ACC1), // Deep aqua sky
    background = Color(0xFF001F24), // Marine dark abyss
    surface = Color(0xFF002F37), // Marine reef dark slate
    onPrimary = Color.White,
    onSecondary = Color(0xFF002F37),
    onBackground = Color(0xFFE0F2F1),
    onSurface = Color(0xFFE0F2F1),
    error = Color(0xFFFF7043)
)

private val OceanBreezeLightColorScheme = lightColorScheme(
    primary = Color(0xFF00796B), // Clean teal emerald
    secondary = Color(0xFF009688), // Clear ocean teal
    tertiary = Color(0xFF0097A7), // Coastal turquoise
    background = Color(0xFFE0F2F1), // Clean foam light aqua
    surface = Color(0xFFF4FBFB), // Soft foam card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF002421),
    onSurface = Color(0xFF002421),
    error = Color(0xFFD84315)
)

// 4. Sunset Ember Theme Colors
private val SunsetEmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF7043), // Sunset orange-red
    secondary = Color(0xFFFFD54F), // Amber glow
    tertiary = Color(0xFFEC407A), // Sunset twilight magenta
    background = Color(0xFF200F12), // Deep wine charcoal
    surface = Color(0xFF2D181C), // Ember dark plum
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFFFF3F0),
    onSurface = Color(0xFFFFF3F0),
    error = Color(0xFFFF5252)
)

private val SunsetEmberLightColorScheme = lightColorScheme(
    primary = Color(0xFFD84315), // Burnt orange sunset
    secondary = Color(0xFFFF8F00), // Amber sun
    tertiary = Color(0xFFC2185B), // Plum twilight
    background = Color(0xFFFFF3E0), // Peach skin white
    surface = Color(0xFFFFF8E1), // Cream sun-kissed
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF3E2723),
    onSurface = Color(0xFF3E2723),
    error = Color(0xFFD84315)
)

@Composable
fun MyApplicationTheme(
    themePreset: AppTheme = AppTheme.CYBERPUNK_DARK,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themePreset) {
        AppTheme.CYBERPUNK_DARK -> if (darkTheme) CyberpunkDarkColorScheme else CyberpunkLightColorScheme
        AppTheme.M3_EXPRESSIVE -> if (darkTheme) M3ExpressiveDarkColorScheme else M3ExpressiveLightColorScheme
        AppTheme.OCEAN_BREEZE -> if (darkTheme) OceanBreezeDarkColorScheme else OceanBreezeLightColorScheme
        AppTheme.SUNSET_EMBER -> if (darkTheme) SunsetEmberDarkColorScheme else SunsetEmberLightColorScheme
    }

    val themeProperties = when (themePreset) {
        AppTheme.CYBERPUNK_DARK -> AppThemeProperties(
            cardShape = RoundedCornerShape(2.dp),
            buttonShape = RoundedCornerShape(4.dp),
            borderWidth = 1.5.dp,
            isBorderGlow = true,
            headerFontFamily = FontFamily.Monospace,
            bodyFontFamily = FontFamily.Monospace,
            containerPadding = 12.dp,
            cardElevation = 0.dp,
            labelWeight = androidx.compose.ui.text.font.FontWeight.Black,
            isAsymmetricStyle = false,
            navigationLayout = "none",
            showScanlineOverlay = true,
            useCompactTechSpecs = true,
            asymmetricBorderAlternator = false
        )
        AppTheme.M3_EXPRESSIVE -> AppThemeProperties(
            cardShape = RoundedCornerShape(28.dp),
            buttonShape = RoundedCornerShape(24.dp),
            borderWidth = 0.dp,
            isBorderGlow = false,
            headerFontFamily = FontFamily.SansSerif,
            bodyFontFamily = FontFamily.SansSerif,
            containerPadding = 20.dp,
            cardElevation = 8.dp,
            labelWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            isAsymmetricStyle = false,
            navigationLayout = "floating_bubble",
            showScanlineOverlay = false,
            useCompactTechSpecs = false,
            asymmetricBorderAlternator = false
        )
        AppTheme.OCEAN_BREEZE -> AppThemeProperties(
            cardShape = RoundedCornerShape(12.dp),
            buttonShape = RoundedCornerShape(16.dp),
            borderWidth = 0.8.dp,
            isBorderGlow = false,
            headerFontFamily = FontFamily.SansSerif,
            bodyFontFamily = FontFamily.SansSerif,
            containerPadding = 16.dp,
            cardElevation = 3.dp,
            labelWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            isAsymmetricStyle = false,
            navigationLayout = "minimal_foam",
            showScanlineOverlay = false,
            useCompactTechSpecs = false,
            asymmetricBorderAlternator = false
        )
        AppTheme.SUNSET_EMBER -> AppThemeProperties(
            cardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 4.dp, bottomStart = 4.dp),
            buttonShape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp),
            borderWidth = 1.2.dp,
            isBorderGlow = false,
            headerFontFamily = FontFamily.Serif,
            bodyFontFamily = FontFamily.Serif,
            containerPadding = 14.dp,
            cardElevation = 4.dp,
            labelWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            isAsymmetricStyle = true,
            navigationLayout = "slanted_asymmetric",
            showScanlineOverlay = false,
            useCompactTechSpecs = false,
            asymmetricBorderAlternator = true
        )
    }

    CompositionLocalProvider(LocalAppThemeProperties provides themeProperties) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
