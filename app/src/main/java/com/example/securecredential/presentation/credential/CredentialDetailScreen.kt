package com.example.securecredential.presentation.credential

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.presentation.authentication.CredentialDisplayState
import com.example.securecredential.presentation.common.ServiceAvatar

/** Spec 13.3: Masked by default, [View] toggles to Displayed, Security Reset always available. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: CredentialDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(uiState.credential?.serviceName.orEmpty()) }) }) { innerPadding ->
        val credential = uiState.credential
        if (uiState.isLoading || credential == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val isDisplayed = uiState.displayState == CredentialDisplayState.DISPLAYED

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceAvatar(credential.serviceName, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(credential.serviceName, style = MaterialTheme.typography.headlineSmall)
                    credential.domain?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }

            Spacer(Modifier.height(24.dp))
            LabeledField("ID", if (isDisplayed) credential.username else credential.usernameMask ?: "••••")
            Spacer(Modifier.height(12.dp))
            LabeledField("Password", if (isDisplayed) credential.password else credential.passwordMask ?: "••••••••")

            credential.category?.let {
                Spacer(Modifier.height(12.dp))
                LabeledField("Category", it)
            }
            credential.memo?.let {
                Spacer(Modifier.height(12.dp))
                LabeledField("Memo", it)
            }

            Spacer(Modifier.height(32.dp))
            Row {
                if (isDisplayed) {
                    OutlinedButton(onClick = viewModel::onSecurityReset) { Text("Hide") }
                } else {
                    Button(onClick = viewModel::onViewClicked) { Text("View") }
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { onEdit(credential.credentialId) }) { Text("Edit") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = viewModel::onDelete) { Text("Delete") }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
