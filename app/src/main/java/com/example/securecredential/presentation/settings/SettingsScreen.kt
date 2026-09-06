package com.example.securecredential.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.data.preferences.ThemeMode
import com.example.securecredential.presentation.authentication.PinDotsIndicator
import com.example.securecredential.presentation.authentication.PinKeypad
import com.example.securecredential.core.util.PinPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenBackupRestore: () -> Unit,
    onLockedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Authentication", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Biometric unlock", modifier = Modifier.fillMaxWidth().padding(end = 8.dp))
                Switch(checked = uiState.biometricEnabled, onCheckedChange = viewModel::setBiometricEnabled)
            }
            TextButton(onClick = viewModel::beginPinChange) { Text("Change PIN") }

            Spacer(Modifier.height(24.dp))
            Text("Auto Lock", style = MaterialTheme.typography.titleLarge)
            listOf(15L to "15 seconds", 30L to "30 seconds", 60L to "1 minute", 300L to "5 minutes").forEach { (seconds, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = uiState.autoLockTimeoutSeconds == seconds, onClick = { viewModel.setAutoLockTimeoutSeconds(seconds) })
                    Text(label)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Theme", style = MaterialTheme.typography.titleLarge)
            ThemeMode.entries.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = uiState.themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                    Text(mode.name)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Backup / Restore", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onOpenBackupRestore) { Text("Manage Backup") }

            Spacer(Modifier.height(24.dp))
            Text("Security", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { viewModel.onLockNow(); onLockedOut() }) { Text("Lock now") }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
            uiState.infoMessage?.let {
                Text(it, modifier = Modifier.padding(top = 16.dp))
            }
        }

        if (uiState.isChangingPin) {
            AlertDialog(
                onDismissRequest = viewModel::cancelPinChange,
                title = {
                    Text(
                        when (uiState.pinChangeStep) {
                            PinChangeStep.OLD_PIN -> "Enter current PIN"
                            PinChangeStep.NEW_PIN -> "Enter new PIN"
                            PinChangeStep.NEW_PIN_CONFIRM -> "Confirm new PIN"
                        }
                    )
                },
                text = {
                    Column {
                        val length = if (uiState.pinChangeStep == PinChangeStep.OLD_PIN) uiState.oldPin.length else uiState.newPin.length
                        PinDotsIndicator(length, PinPolicy.MIN_NUMERIC_LENGTH)
                        Spacer(Modifier.height(16.dp))
                        PinKeypad(onDigit = viewModel::appendPinChangeDigit, onBackspace = viewModel::pinChangeBackspace)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        when (uiState.pinChangeStep) {
                            PinChangeStep.OLD_PIN -> viewModel.submitOldPin()
                            PinChangeStep.NEW_PIN -> viewModel.submitNewPin()
                            PinChangeStep.NEW_PIN_CONFIRM -> viewModel.submitNewPinConfirmation()
                        }
                    }) { Text("Next") }
                },
                dismissButton = { TextButton(onClick = viewModel::cancelPinChange) { Text("Cancel") } }
            )
        }
    }
}
