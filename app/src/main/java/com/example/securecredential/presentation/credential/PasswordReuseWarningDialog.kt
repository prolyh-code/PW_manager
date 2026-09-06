package com.example.securecredential.presentation.credential

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.securecredential.presentation.common.theme.CautionAmber

/**
 * Spec 7 + 13.1: a boolean-driven prompt only — never reveals which credential matched, and
 * deliberately amber/caution-toned rather than warning-red (this app never uses red styling,
 * even here; a reused password is a caution nudge, not an error state).
 */
@Composable
fun PasswordReuseWarningDialog(onDismiss: () -> Unit, onContinueAnyway: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("This password is already in use", color = CautionAmber) },
        text = { Text("You've used this password for another saved credential. Consider using a unique one.") },
        confirmButton = {
            TextButton(onClick = onContinueAnyway) { Text("Use anyway") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Change password") }
        }
    )
}
