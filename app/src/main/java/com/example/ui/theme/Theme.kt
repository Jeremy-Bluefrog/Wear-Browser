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
    primary = Color(0xFFA5EEFD),
    onPrimary = Color(0xFF00373D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFFA5EEFD),
    secondary = Color(0xFFB1CBD0),
    onSecondary = Color(0xFF1C3438),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFFA5EEFD),
    tertiary = Color(0xFFBAC6EA),
    onTertiary = Color(0xFF24304D),
    tertiaryContainer = Color(0xFF3B4664),
    onTertiaryContainer = Color(0xFFD9E2FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000), // Pure black for OLED
    onBackground = Color(0xFFE0E3E3),
    outline = Color(0xFF899294),
    outlineVariant = Color(0xFF3F484A),
    surfaceContainerLow = Color(0xFF161D1E),
    surfaceContainer = Color(0xFF1E262B),
    surfaceContainerHigh = Color(0xFF252F34),
    onSurface = Color(0xFFE0E3E3),
    onSurfaceVariant = Color(0xFFBFC8CA)
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
