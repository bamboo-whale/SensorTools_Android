package com.sensortools.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = Background,
    primaryContainer = CardBackground,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = Background,
    secondaryContainer = CardBackgroundAlt,
    onSecondaryContainer = TextSecondary,
    tertiary = TextTertiary,
    onTertiary = Background,
    background = Background,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = CardBackgroundAlt,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = BorderLight,
    error = StatusError,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightTextPrimary,
    onPrimary = LightBackground,
    primaryContainer = LightCardBackground,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightTextSecondary,
    onSecondary = LightBackground,
    secondaryContainer = LightCardBackgroundAlt,
    onSecondaryContainer = LightTextSecondary,
    tertiary = LightTextTertiary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightCardBackground,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardBackgroundAlt,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderLight,
    error = StatusError,
    onError = TextPrimary
)

@Composable
fun SensorToolsTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
