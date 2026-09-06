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

private val VibrantColorScheme = lightColorScheme(
    primary = VibrantPurple,
    onPrimary = Color.White,
    primaryContainer = VibrantPurpleContainer,
    onPrimaryContainer = VibrantDeepPurple,
    secondary = VibrantPurple,
    onSecondary = Color.White,
    secondaryContainer = VibrantSurfaceVariant,
    onSecondaryContainer = VibrantTextPrimary,
    tertiary = VibrantDeepPurple,
    onTertiary = Color.White,
    background = VibrantBackground,
    onBackground = VibrantTextPrimary,
    surface = VibrantSurface,
    onSurface = VibrantTextPrimary,
    surfaceVariant = VibrantSurfaceVariant,
    onSurfaceVariant = VibrantTextSecondary,
    outline = VibrantOutline,
    outlineVariant = VibrantBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF2B2930),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to the signature Vibrant Palette (#FDF7FF)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> VibrantColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
