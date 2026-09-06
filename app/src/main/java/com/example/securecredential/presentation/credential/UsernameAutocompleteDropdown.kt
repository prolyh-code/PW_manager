package com.example.securecredential.presentation.credential

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.securecredential.domain.model.CredentialSummary

/**
 * Spec 8.4 — Exact Match ID suggestion. Because the search is HMAC-exact (no partial/prefix
 * match, spec 8.3), a suggestion only ever appears once the typed [currentUsername] already
 * matches another stored credential's username exactly — so "selecting" a suggestion is really
 * an acknowledgement ("yes, reuse this ID") rather than filling in new text. Only the username
 * is ever reused; password/mask are never copied (spec 8.4).
 */
@Composable
fun UsernameAutocompleteDropdown(
    suggestions: List<CredentialSummary>,
    currentUsername: String,
    onSelect: (username: String) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = suggestions.isNotEmpty(), onDismissRequest = onDismiss) {
        suggestions.forEach { summary ->
            DropdownMenuItem(
                text = { Text("${summary.usernameMask ?: "••••"} · already used for ${summary.serviceName}") },
                onClick = { onSelect(currentUsername) }
            )
        }
    }
}
