package com.example.securecredential.presentation.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.core.util.PinPolicy
import com.example.securecredential.presentation.authentication.PinDotsIndicator
import com.example.securecredential.presentation.authentication.PinKeypad

/** Spec 13.3: Last Backup status + Restore's PIN entry flow (spec 5.4 Restore sequence). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onRestoreComplete: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.restoreComplete) {
        if (uiState.restoreComplete) onRestoreComplete()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Backup / Restore") }) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
            Text(
                "Backup is handled automatically by Android (Cloud Backup / Device-to-Device). " +
                    "If you're on a new device with restored app data, enter your original PIN below " +
                    "to recover your credentials.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(32.dp))

            if (uiState.isRestoring) {
                CircularProgressIndicator()
            } else {
                Text("Restore with PIN", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                PinDotsIndicator(uiState.restorePin.length, PinPolicy.MIN_NUMERIC_LENGTH)
                Spacer(Modifier.height(16.dp))
                PinKeypad(onDigit = viewModel::appendPinDigit, onBackspace = viewModel::backspace)
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::submitRestorePin, enabled = uiState.restorePin.isNotEmpty()) {
                    Text("Restore")
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
