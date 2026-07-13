package com.example.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.core.content.ContextCompat

import androidx.compose.material3.dynamicDarkColorScheme

private val WearDarkColorScheme = ColorScheme(
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
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF000000), // Pure black for OLED
    onBackground = Color(0xFFE6E1E5),
    outline = Color(0xFF938F99)
)

@Composable
fun WearAppTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicColor = dynamicDarkColorScheme(context)
        // Convert regular M3 ColorScheme to Wear M3 ColorScheme
        ColorScheme(
            primary = dynamicColor.primary,
            onPrimary = dynamicColor.onPrimary,
            primaryContainer = dynamicColor.primaryContainer,
            onPrimaryContainer = dynamicColor.onPrimaryContainer,
            secondary = dynamicColor.secondary,
            onSecondary = dynamicColor.onSecondary,
            secondaryContainer = dynamicColor.secondaryContainer,
            onSecondaryContainer = dynamicColor.onSecondaryContainer,
            tertiary = dynamicColor.tertiary,
            onTertiary = dynamicColor.onTertiary,
            tertiaryContainer = dynamicColor.tertiaryContainer,
            onTertiaryContainer = dynamicColor.onTertiaryContainer,
            error = dynamicColor.error,
            onError = dynamicColor.onError,
            errorContainer = dynamicColor.errorContainer,
            onErrorContainer = dynamicColor.onErrorContainer,
            background = Color.Black, // Keep pure black for Wear
            onBackground = dynamicColor.onBackground,
            outline = dynamicColor.outline,
            outlineVariant = dynamicColor.outlineVariant,
            surfaceContainerLow = dynamicColor.surfaceContainerLow,
            surfaceContainer = dynamicColor.surfaceContainer,
            surfaceContainerHigh = dynamicColor.surfaceContainerHigh,
            onSurface = dynamicColor.onSurface,
            onSurfaceVariant = dynamicColor.onSurfaceVariant
        )
    } else {
        WearDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
