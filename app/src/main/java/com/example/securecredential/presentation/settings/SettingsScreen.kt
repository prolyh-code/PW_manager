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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.R
import com.example.securecredential.core.util.AppLanguage
import com.example.securecredential.core.util.PinPolicy
import com.example.securecredential.core.util.asString
import com.example.securecredential.data.preferences.ThemeMode
import com.example.securecredential.presentation.authentication.PinDotsIndicator
import com.example.securecredential.presentation.authentication.PinKeypad
import com.example.securecredential.presentation.common.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onLockedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = { BackButton(onClick = onBack) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(R.string.section_authentication), style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.label_biometric_unlock), modifier = Modifier.fillMaxWidth().padding(end = 8.dp))
                Switch(checked = uiState.biometricEnabled, onCheckedChange = viewModel::setBiometricEnabled)
            }
            TextButton(onClick = viewModel::beginPinChange) { Text(stringResource(R.string.action_change_pin)) }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.security_setup_autolock_title), style = MaterialTheme.typography.titleLarge)
            listOf(
                15L to stringResource(R.string.autolock_option_15s),
                30L to stringResource(R.string.autolock_option_30s),
                60L to stringResource(R.string.autolock_option_1m),
                300L to stringResource(R.string.autolock_option_5m)
            ).forEach { (seconds, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = uiState.autoLockTimeoutSeconds == seconds, onClick = { viewModel.setAutoLockTimeoutSeconds(seconds) })
                    Text(label)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.section_theme), style = MaterialTheme.typography.titleLarge)
            listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark)
            ).forEach { (mode, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = uiState.themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                    Text(label)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.section_language), style = MaterialTheme.typography.titleLarge)
            listOf(
                AppLanguage.KOREAN to stringResource(R.string.language_korean),
                AppLanguage.ENGLISH to stringResource(R.string.language_english)
            ).forEach { (language, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = uiState.language == language, onClick = { viewModel.setLanguage(language) })
                    Text(label)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.section_backup_restore), style = MaterialTheme.typography.titleLarge)
            Button(onClick = onOpenBackupRestore) { Text(stringResource(R.string.action_manage_backup)) }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.section_security), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { viewModel.onLockNow(); onLockedOut() }) { Text(stringResource(R.string.action_lock_now)) }

            uiState.errorMessage?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
            uiState.infoMessage?.let {
                Text(it.asString(), modifier = Modifier.padding(top = 16.dp))
            }
        }

        if (uiState.isChangingPin) {
            AlertDialog(
                onDismissRequest = viewModel::cancelPinChange,
                title = {
                    Text(
                        stringResource(
                            when (uiState.pinChangeStep) {
                                PinChangeStep.OLD_PIN -> R.string.pin_change_title_old
                                PinChangeStep.NEW_PIN -> R.string.pin_change_title_new
                                PinChangeStep.NEW_PIN_CONFIRM -> R.string.pin_change_title_confirm
                            }
                        )
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
                    }) { Text(stringResource(R.string.action_next)) }
                },
                dismissButton = { TextButton(onClick = viewModel::cancelPinChange) { Text(stringResource(R.string.action_cancel)) } }
            )
        }
    }
}
