package com.example.securecredential.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.securecredential.domain.model.CredentialSummary

/** Shared by Home's Recent list and the Search Result screen — spec 13.3's card list, CredentialSummary only. */
@Composable
fun CredentialSummaryRow(summary: CredentialSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ServiceAvatar(summary.serviceName)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(summary.serviceName, style = MaterialTheme.typography.titleLarge)
                summary.domain?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    "${summary.usernameMask ?: "••••"} · ${summary.passwordMask ?: "••••"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
