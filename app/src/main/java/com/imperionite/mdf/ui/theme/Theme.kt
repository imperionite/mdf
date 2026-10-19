package com.imperionite.mdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MdfTeal,
    onPrimary = MdfInk,
    primaryContainer = MdfPaleBlue,
    onPrimaryContainer = MdfInk,
    secondary = MdfBlueGray,
    onSecondary = MdfInk,
    secondaryContainer = MdfLightGray,
    onSecondaryContainer = MdfInk,
    tertiary = MdfPowderBlue,
    onTertiary = MdfInk,
    tertiaryContainer = MdfPaleBlue,
    onTertiaryContainer = MdfInk,
    error = MdfError,
    onError = MdfWhite,
    background = MdfLightGray,
    onBackground = MdfInk,
    surface = MdfWhite,
    onSurface = MdfInk,
    surfaceVariant = MdfLightGray,
    onSurfaceVariant = MdfMutedInk,
    outline = MdfBlueGray,
    outlineVariant = MdfBlueGray.copy(alpha = 0.55f),
    surfaceTint = MdfTeal
)

private val DarkColorScheme = darkColorScheme(
    primary = MdfTeal,
    onPrimary = MdfInk,
    primaryContainer = Color(0xFF34515B),
    onPrimaryContainer = MdfDarkText,
    secondary = MdfBlueGray,
    onSecondary = MdfInk,
    secondaryContainer = MdfDarkSurfaceVariant,
    onSecondaryContainer = MdfDarkText,
    tertiary = MdfPowderBlue,
    onTertiary = MdfInk,
    tertiaryContainer = MdfDarkSurfaceVariant,
    onTertiaryContainer = MdfDarkText,
    error = MdfDarkError,
    onError = MdfDarkBackground,
    background = MdfDarkBackground,
    onBackground = MdfDarkText,
    surface = MdfDarkSurface,
    onSurface = MdfDarkText,
    surfaceVariant = MdfDarkSurfaceVariant,
    onSurfaceVariant = MdfDarkMutedText,
    outline = MdfBlueGray,
    outlineVariant = MdfDarkSurfaceVariant,
    surfaceTint = MdfTeal
)

@Suppress("FunctionName")
@Composable
fun MDFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Use MDF's own palette in both modes rather than Android wallpaper colors.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
