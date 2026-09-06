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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
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

    LaunchedEffect(uiState.unlocked) {
        if (uiState.unlocked) onUnlocked()
    }

    fun startAuthentication(allowedAuthenticators: Int) {
        scope.launch {
            val cipher = viewModel.prepareCipher().getOrElse {
                viewModel.onAuthenticationFailed(it.message)
                return@launch
            }
            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock SecureVault")
                .setAllowedAuthenticators(allowedAuthenticators)
            // A negative ("Cancel") button is only valid when a biometric class is allowed;
            // DEVICE_CREDENTIAL-only prompts provide their own system back-navigation instead.
            if (allowedAuthenticators == BiometricManager.Authenticators.BIOMETRIC_STRONG) {
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }

            when (val result = biometricAuthManager.authenticate(
                promptInfoBuilder.build(),
                BiometricPrompt.CryptoObject(cipher)
            )) {
                is BiometricResult.Success -> viewModel.onAuthenticationSucceeded(result.cryptoObject?.cipher ?: cipher)
                is BiometricResult.Failed -> viewModel.onAuthenticationFailed("Not recognized — try again")
                is BiometricResult.Cancelled -> Unit
                is BiometricResult.Error -> viewModel.onAuthenticationFailed("Authentication error")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SecureVault", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(48.dp))

        if (uiState.isAuthenticating) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { startAuthentication(BiometricManager.Authenticators.BIOMETRIC_STRONG) }) {
                Text("Use fingerprint")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { startAuthentication(BiometricManager.Authenticators.DEVICE_CREDENTIAL) }) {
                Text("Use PIN")
            }
        }

        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 24.dp))
        }
    }
}
