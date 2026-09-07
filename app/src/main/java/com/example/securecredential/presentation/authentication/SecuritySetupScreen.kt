package com.example.securecredential.presentation.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.R
import com.example.securecredential.core.util.asString

@Composable
fun SecuritySetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SecuritySetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) onSetupComplete()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (uiState.step) {
            SecuritySetupStep.BIOMETRIC -> BiometricStep(uiState, viewModel)
            SecuritySetupStep.PIN_CREATE -> PinCreateStep(uiState, viewModel)
            SecuritySetupStep.PIN_CONFIRM -> PinConfirmStep(uiState, viewModel)
            SecuritySetupStep.AUTO_LOCK -> AutoLockStep(uiState, viewModel)
        }
        uiState.errorMessage?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun BiometricStep(uiState: SecuritySetupUiState, viewModel: SecuritySetupViewModel) {
    Text(
        stringResource(R.string.security_setup_biometric_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.security_setup_biometric_toggle_label))
        Spacer(Modifier.height(0.dp))
        Switch(checked = uiState.biometricEnabled, onCheckedChange = viewModel::setBiometricEnabled)
    }
    Spacer(Modifier.height(32.dp))
    Button(onClick = { viewModel.goToStep(SecuritySetupStep.PIN_CREATE) }) { Text(stringResource(R.string.action_continue)) }
}

@Composable
private fun PinCreateStep(uiState: SecuritySetupUiState, viewModel: SecuritySetupViewModel) {
    Text(
        stringResource(R.string.security_setup_pin_create_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 48.dp)
    )
    Text(
        stringResource(R.string.security_setup_pin_hint, SecuritySetupViewModel.MIN_NUMERIC_LENGTH),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    PinDotsIndicator(
        uiState.pin.length, SecuritySetupViewModel.MIN_NUMERIC_LENGTH,
        modifier = Modifier.padding(vertical = 24.dp)
    )
    PinKeypad(onDigit = viewModel::appendDigit, onBackspace = viewModel::backspace)
    Spacer(Modifier.height(24.dp))
    Button(onClick = viewModel::confirmPinAndProceed, enabled = uiState.pin.isNotEmpty()) {
        Text(stringResource(R.string.action_continue))
    }
}

@Composable
private fun PinConfirmStep(uiState: SecuritySetupUiState, viewModel: SecuritySetupViewModel) {
    Text(
        stringResource(R.string.security_setup_pin_confirm_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
    )
    PinDotsIndicator(
        uiState.pinConfirmation.length, SecuritySetupViewModel.MIN_NUMERIC_LENGTH,
        modifier = Modifier.padding(bottom = 24.dp)
    )
    PinKeypad(onDigit = viewModel::appendDigit, onBackspace = viewModel::backspace)
    Spacer(Modifier.height(24.dp))
    Button(onClick = viewModel::submitPinConfirmationAndProceed, enabled = uiState.pinConfirmation.isNotEmpty()) {
        Text(stringResource(R.string.action_continue))
    }
}

@Composable
private fun AutoLockStep(uiState: SecuritySetupUiState, viewModel: SecuritySetupViewModel) {
    Text(
        stringResource(R.string.security_setup_autolock_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
    )
    val options = listOf(
        15L to stringResource(R.string.autolock_option_15s),
        30L to stringResource(R.string.autolock_option_30s),
        60L to stringResource(R.string.autolock_option_1m),
        300L to stringResource(R.string.autolock_option_5m)
    )
    options.forEach { (seconds, label) ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            RadioButton(selected = uiState.autoLockTimeoutSeconds == seconds, onClick = { viewModel.setAutoLockTimeoutSeconds(seconds) })
            Text(label)
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = viewModel::finishSetup, enabled = !uiState.isSubmitting) {
        if (uiState.isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.height(18.dp))
        } else {
            Text(stringResource(R.string.action_finish_setup))
        }
    }
}
