package com.example.securecredential.data.security

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Never carries the raw AuthenticationResult or any log-worthy detail (spec invariant #6). */
sealed class BiometricResult {
    data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : BiometricResult()
    data object Failed : BiometricResult()
    data class Error(val errorCode: Int) : BiometricResult()
    data object Cancelled : BiometricResult()
}

/**
 * Thin coroutine wrapper around [BiometricPrompt]. Requires a [FragmentActivity] host — see
 * MainActivity's comment. Real hardware/enrolled-biometric interaction is out of automated-test
 * scope (plan file); this class is verified manually.
 */
class BiometricAuthManager(private val activity: FragmentActivity) {

    suspend fun authenticate(
        promptInfo: BiometricPrompt.PromptInfo,
        cryptoObject: BiometricPrompt.CryptoObject
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                continuation.resume(BiometricResult.Success(result.cryptoObject))
            }

            override fun onAuthenticationFailed() {
                continuation.resume(BiometricResult.Failed)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val isUserCancel = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                continuation.resume(
                    if (isUserCancel) BiometricResult.Cancelled else BiometricResult.Error(errorCode)
                )
            }
        }
        BiometricPrompt(activity, executor, callback).authenticate(promptInfo, cryptoObject)
    }
}
