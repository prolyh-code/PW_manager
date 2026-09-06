package com.example.securecredential.presentation.common.theme

import androidx.compose.ui.graphics.Color

// Spec 13.1 style language: purple-gradient, soft shadows. Deliberately NOT a warning-red
// anywhere (a displayed/unmasked credential is a normal state, not an error state).
val PurplePrimary = Color(0xFF6C5CE7)
val PurplePrimaryDark = Color(0xFF4834D4)
val PurpleContainer = Color(0xFFE9E4FF)
val PurpleContainerDark = Color(0xFF2A2150)

val VaultBackground = Color(0xFFFAF9FF)
val VaultBackgroundDark = Color(0xFF16132B)
val VaultSurface = Color(0xFFFFFFFF)
val VaultSurfaceDark = Color(0xFF201C3D)

val VaultOnPrimary = Color(0xFFFFFFFF)
val VaultOnBackground = Color(0xFF1C1B2E)
val VaultOnBackgroundDark = Color(0xFFEDEBFF)

// Non-alarming caution accent for the Password Reuse warning (spec 13.1: no red even here).
val CautionAmber = Color(0xFFE8A33D)
