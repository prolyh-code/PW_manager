package com.example.securecredential.data.security

import com.example.securecredential.core.crypto.Pbkdf2KeyDeriver
import com.example.securecredential.core.crypto.SymmetricKeyGenerator
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryKeyManagerTest {

    private val recoveryKeyManager = RecoveryKeyManager(Pbkdf2KeyDeriver())
    private val testIterations = 1_000 // fast for tests; production uses DEFAULT_ITERATIONS

    @Test
    fun `correct pin recovers the exact original app encryption key`() {
        val appEncryptionKey = SymmetricKeyGenerator.generateAesKey()
        val salt = recoveryKeyManager.generateSalt()
        val pin = "MySecurePin123".toCharArray()
        val wrapped = recoveryKeyManager.createRecoveryWrap(appEncryptionKey, pin, salt, testIterations)

        val result = recoveryKeyManager.unwrapRecovery(
            wrapped = wrapped,
            salt = salt,
            pin = pin,
            iterations = testIterations,
            consecutiveFailedAttempts = 0,
            lastFailureAt = null
        )

        assertTrue(result.isSuccess)
        assertTrue(appEncryptionKey.encoded.contentEquals(result.getOrThrow().encoded))
    }

    @Test
    fun `wrong pin fails with PinMismatch not a generic crash`() {
        val appEncryptionKey = SymmetricKeyGenerator.generateAesKey()
        val salt = recoveryKeyManager.generateSalt()
        val wrapped = recoveryKeyManager.createRecoveryWrap(
            appEncryptionKey, "CorrectPin123".toCharArray(), salt, testIterations
        )

        val result = recoveryKeyManager.unwrapRecovery(
            wrapped = wrapped,
            salt = salt,
            pin = "WrongPin456".toCharArray(),
            iterations = testIterations,
            consecutiveFailedAttempts = 0,
            lastFailureAt = null
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RecoveryUnwrapError.PinMismatch)
    }

    @Test
    fun `attempt within backoff window is rejected before touching the pin`() {
        val appEncryptionKey = SymmetricKeyGenerator.generateAesKey()
        val salt = recoveryKeyManager.generateSalt()
        val pin = "MySecurePin123".toCharArray()
        val wrapped = recoveryKeyManager.createRecoveryWrap(appEncryptionKey, pin, salt, testIterations)
        val now = Instant.now()

        // 5 consecutive failures -> 30s backoff (RecoveryBackoffPolicy); only 1s has elapsed.
        val result = recoveryKeyManager.unwrapRecovery(
            wrapped = wrapped,
            salt = salt,
            pin = pin, // even the CORRECT pin must be rejected while the window is active
            iterations = testIterations,
            consecutiveFailedAttempts = 5,
            lastFailureAt = now.minusSeconds(1),
            now = now
        )

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is RecoveryUnwrapError.AttemptLimited)
        assertEquals(29L, (error as RecoveryUnwrapError.AttemptLimited).retryAfterSeconds)
    }

    @Test
    fun `attempt after backoff window elapses is allowed`() {
        val appEncryptionKey = SymmetricKeyGenerator.generateAesKey()
        val salt = recoveryKeyManager.generateSalt()
        val pin = "MySecurePin123".toCharArray()
        val wrapped = recoveryKeyManager.createRecoveryWrap(appEncryptionKey, pin, salt, testIterations)
        val now = Instant.now()

        val result = recoveryKeyManager.unwrapRecovery(
            wrapped = wrapped,
            salt = salt,
            pin = pin,
            iterations = testIterations,
            consecutiveFailedAttempts = 5,
            lastFailureAt = now.minusSeconds(31),
            now = now
        )

        assertTrue(result.isSuccess)
    }
}
