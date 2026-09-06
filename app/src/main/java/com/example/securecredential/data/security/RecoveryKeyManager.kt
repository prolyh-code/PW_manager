package com.example.securecredential.data.security

import com.example.securecredential.core.crypto.AeadIntegrityException
import com.example.securecredential.core.crypto.AesGcmCipher
import com.example.securecredential.core.crypto.EncryptedBlob
import com.example.securecredential.core.crypto.PinKeyDeriver
import com.example.securecredential.core.crypto.RecoveryBackoffPolicy
import java.security.SecureRandom
import java.time.Instant
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/** Distinguishes a wrong PIN (KEY-005) from a technical failure (KEY-004) per spec 12/5.4. */
sealed class RecoveryUnwrapError : Exception() {
    data object PinMismatch : RecoveryUnwrapError()
    data class TechnicalFailure(override val cause: Throwable) : RecoveryUnwrapError()
    /** Backoff window not yet elapsed (KEY-006, spec 5.2). */
    data class AttemptLimited(val retryAfterSeconds: Long) : RecoveryUnwrapError()
}

/**
 * Implements the Recovery Wrapped Key path only (spec 5.1/5.2) — independent of Android
 * Keystore, so a wrong PIN is defended purely by KDF cost. No Android framework dependency,
 * so this class is exercised by plain JUnit tests, not instrumented ones.
 */
class RecoveryKeyManager(private val pinKeyDeriver: PinKeyDeriver) {

    companion object {
        private const val SALT_LENGTH_BYTES = 16
        private const val OUTPUT_KEY_ALGORITHM = "AES"
    }

    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

    fun createRecoveryWrap(
        appEncryptionKey: SecretKey,
        pin: CharArray,
        salt: ByteArray,
        iterations: Int
    ): EncryptedBlob {
        val recoveryKek = pinKeyDeriver.deriveKey(pin, salt, iterations)
        return AesGcmCipher.encrypt(recoveryKek, appEncryptionKey.encoded)
    }

    fun unwrapRecovery(
        wrapped: EncryptedBlob,
        salt: ByteArray,
        pin: CharArray,
        iterations: Int,
        consecutiveFailedAttempts: Int,
        lastFailureAt: Instant?,
        now: Instant = Instant.now()
    ): Result<SecretKey> {
        val requiredDelayMillis = RecoveryBackoffPolicy.delayForAttempt(consecutiveFailedAttempts).inWholeMilliseconds
        if (lastFailureAt != null && requiredDelayMillis > 0) {
            val elapsedMillis = now.toEpochMilli() - lastFailureAt.toEpochMilli()
            if (elapsedMillis < requiredDelayMillis) {
                val remainingSeconds = (requiredDelayMillis - elapsedMillis) / 1000
                return Result.failure(RecoveryUnwrapError.AttemptLimited(remainingSeconds))
            }
        }

        val recoveryKek = pinKeyDeriver.deriveKey(pin, salt, iterations)
        return try {
            val rawKey = AesGcmCipher.decrypt(recoveryKek, wrapped)
            Result.success(SecretKeySpec(rawKey, OUTPUT_KEY_ALGORITHM))
        } catch (e: AeadIntegrityException) {
            Result.failure(RecoveryUnwrapError.PinMismatch)
        } catch (e: Exception) {
            Result.failure(RecoveryUnwrapError.TechnicalFailure(e))
        }
    }
}
