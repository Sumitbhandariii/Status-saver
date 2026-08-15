package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StatusVaultColorScheme = lightColorScheme(
    primary = PrimaryDeepPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleContainer,
    onPrimaryContainer = PrimaryPurpleVariant,
    secondary = SecondaryCyan,
    onSecondary = Color.White,
    secondaryContainer = SecondaryCyanContainer,
    onSecondaryContainer = SecondaryCyanDark,
    tertiary = AccentAmber,
    onTertiary = Color.White,
    tertiaryContainer = AccentAmberLight,
    onTertiaryContainer = Color(0xFF8A5B00),
    background = AppBackground,
    onBackground = PrimaryText,
    surface = AppSurface,
    onSurface = PrimaryText,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = SecondaryText,
    outline = BorderSubtle,
    outlineVariant = DividerColor,
    error = AccentRed,
    onError = Color.White,
    errorContainer = AccentRedLight,
    onErrorContainer = AccentRed
)

@Composable
fun StatusVaultTheme(
    content: @Composable () -> Unit
) {
    // Strictly Light Mode Only as per requirements
    MaterialTheme(
        colorScheme = StatusVaultColorScheme,
        typography = Typography,
        content = content
    )
}

