package com.example.securecredential.presentation.common

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * A back arrow for TopAppBar's navigationIcon slot. Plain text glyph rather than
 * androidx.compose.material.icons (core icon set isn't a guaranteed transitive dependency of
 * material3 in the pinned BOM) — same pattern already used for the PIN keypad's backspace key.
 */
@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Text("←", style = MaterialTheme.typography.headlineSmall)
    }
}
