package com.example.securecredential.presentation.authentication

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.R
import com.example.securecredential.core.util.UiText
import com.example.securecredential.core.util.asString
import com.example.securecredential.data.security.BiometricAuthManager
import com.example.securecredential.data.security.BiometricResult
import kotlinx.coroutines.launch

/**
 * Spec 13.3 — "central, Use fingerprint / Use PIN". Both buttons authorize the SAME
 * Keystore-gated Local Wrapped Key (spec 5.2/5.3): "Use fingerprint" restricts BiometricPrompt
 * to BIOMETRIC_STRONG, "Use PIN" restricts it to DEVICE_CREDENTIAL (the system's own
 * PIN/pattern/password screen — not this app's custom keypad, deliberately reusing the OS's
 * own hardware-backed lockout instead of the app's slow PBKDF2-gated Recovery path).
 */
@Composable
fun AuthenticationScreen(
    onUnlocked: () -> Unit,
    viewModel: AuthenticationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as FragmentActivity
    val biometricAuthManager = remember { BiometricAuthManager(activity) }
    val scope = rememberCoroutineScope()

    // Captured here (a @Composable context) since these are used by real Android APIs
    // (BiometricPrompt) that need an already-resolved String, not a UiText.
    val unlockPromptTitle = stringResource(R.string.unlock_prompt_title, stringResource(R.string.app_name))
    val cancelLabel = stringResource(R.string.action_cancel)

    LaunchedEffect(uiState.unlocked) {
        if (uiState.unlocked) onUnlocked()
    }

    fun startAuthentication(allowedAuthenticators: Int) {
        scope.launch {
            val cipher = viewModel.prepareCipher().getOrElse {
                viewModel.onAuthenticationFailed(it.message?.let { m -> UiText.Dynamic(m) })
                return@launch
            }
            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(unlockPromptTitle)
                .setAllowedAuthenticators(allowedAuthenticators)
            // A negative ("Cancel") button is only valid when a biometric class is allowed;
            // DEVICE_CREDENTIAL-only prompts provide their own system back-navigation instead.
            if (allowedAuthenticators == BiometricManager.Authenticators.BIOMETRIC_STRONG) {
                promptInfoBuilder.setNegativeButtonText(cancelLabel)
            }

            when (val result = biometricAuthManager.authenticate(
                promptInfoBuilder.build(),
                BiometricPrompt.CryptoObject(cipher)
            )) {
                is BiometricResult.Success -> viewModel.onAuthenticationSucceeded(result.cryptoObject?.cipher ?: cipher)
                is BiometricResult.Failed -> viewModel.onAuthenticationFailed(UiText.of(R.string.error_biometric_not_recognized))
                is BiometricResult.Cancelled -> Unit
                is BiometricResult.Error -> viewModel.onAuthenticationFailed(UiText.of(R.string.error_authentication_generic))
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(48.dp))

        if (uiState.isAuthenticating) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { startAuthentication(BiometricManager.Authenticators.BIOMETRIC_STRONG) }) {
                Text(stringResource(R.string.action_use_fingerprint))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { startAuthentication(BiometricManager.Authenticators.DEVICE_CREDENTIAL) }) {
                Text(stringResource(R.string.action_use_pin))
            }
        }

        uiState.errorMessage?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 24.dp))
        }
    }
}
