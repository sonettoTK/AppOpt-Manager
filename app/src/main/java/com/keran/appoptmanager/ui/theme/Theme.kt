package com.keran.appoptmanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SoftTeal,
    onPrimary = White,
    primaryContainer = OutlineGray.copy(alpha = 0.5f),
    onPrimaryContainer = MutedGray,
    secondary = WarmGray,
    onSecondary = SoftCharcoal,
    secondaryContainer = TagThreadBg,
    onSecondaryContainer = TagThreadText,
    tertiary = TagMainText,
    tertiaryContainer = TagMainBg,
    onTertiaryContainer = TagMainText,
    background = WarmCream,
    onBackground = SoftCharcoal,
    surface = White,
    onSurface = SoftCharcoal,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedGray,
    outline = OutlineGray,
    outlineVariant = OutlineGray.copy(alpha = 0.5f),
    inverseSurface = TagSubBg,
    inverseOnSurface = TagSubText,
    error = SoftTerracotta,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHigh,
    surfaceContainerLow = DarkSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkTagSubBg,
    inverseOnSurface = DarkTagSubText,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

@Composable
fun AppOptManagerTheme(
    themeMode: String = "system",
    uiOpacity: Float = 1.0f,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (uiOpacity < 1.0f) {
        baseScheme.copy(
            surface = baseScheme.surface.copy(alpha = uiOpacity),
            surfaceVariant = baseScheme.surfaceVariant.copy(alpha = uiOpacity),
            background = baseScheme.background.copy(alpha = uiOpacity),
            surfaceContainer = baseScheme.surfaceContainer.copy(alpha = uiOpacity),
            surfaceContainerHigh = baseScheme.surfaceContainerHigh.copy(alpha = uiOpacity),
            surfaceContainerHighest = baseScheme.surfaceContainerHighest.copy(alpha = uiOpacity),
            surfaceContainerLow = baseScheme.surfaceContainerLow.copy(alpha = uiOpacity),
            surfaceContainerLowest = baseScheme.surfaceContainerLowest.copy(alpha = uiOpacity)
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
