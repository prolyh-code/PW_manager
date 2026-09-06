package com.example.securecredential.presentation.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = VaultOnPrimary,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = PurplePrimaryDark,
    background = VaultBackground,
    onBackground = VaultOnBackground,
    surface = VaultSurface,
    onSurface = VaultOnBackground
)

private val DarkColors = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = VaultOnPrimary,
    primaryContainer = PurpleContainerDark,
    onPrimaryContainer = PurpleContainer,
    background = VaultBackgroundDark,
    onBackground = VaultOnBackgroundDark,
    surface = VaultSurfaceDark,
    onSurface = VaultOnBackgroundDark
)

// Spec 13.1: "big Rounded Card" — a noticeably larger corner radius than stock Material.
private val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun SecureVaultTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = VaultTypography,
        shapes = VaultShapes,
        content = content
    )
}
