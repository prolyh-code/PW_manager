package com.example.securecredential.presentation.credential

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.securecredential.R
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
        title = { Text(stringResource(R.string.reuse_dialog_title), color = CautionAmber) },
        text = { Text(stringResource(R.string.reuse_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onContinueAnyway) { Text(stringResource(R.string.action_use_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_change_password)) }
        }
    )
}
