package com.example.securecredential.data.security

import com.example.securecredential.core.crypto.AesGcmCipher
import com.example.securecredential.core.crypto.EncryptedBlob
import com.example.securecredential.core.crypto.Pbkdf2KeyDeriver
import com.example.securecredential.core.crypto.PinVerificationHasher
import com.example.securecredential.core.crypto.SymmetricKeyGenerator
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/** Orchestrator-level failures distinct from [RecoveryUnwrapError] (which is Recovery-path only). */
sealed class KeyLifecycleError : Exception() {
    data object OldPinIncorrect : KeyLifecycleError()
    data object NotInitialized : KeyLifecycleError()
}

data class RecoveredKeys(
    val appEncryptionKey: SecretKey,
    val searchHmacKey: SecretKey,
    val idIndexKey: SecretKey
)

/**
 * Encodes the exact Key Lifecycle sequences from spec 5.4 as named functions, so no call site
 * can reorder or skip a step (Device Master Key -> App Encryption Key -> Search HMAC Key ->
 * Id Index Key -> Local Wrap -> PIN -> Recovery Salt/KEK/Wrap -> [Biometric] -> READY).
 *
 * Design notes (not fully spelled out in the spec):
 * - The Search HMAC Key and Id Index Key (spec 6.1's separate username-index key) are each
 *   encrypted with the App Encryption Key (AES-GCM) rather than given their own Local/Recovery
 *   dual-wrap — this keeps the wrap surface to one scheme while all three keys remain
 *   cryptographically distinct (invariant #3, and the domain/service index staying unlinkable
 *   from the username index).
 * - All functions are suspend because [KeyMaterialStore] is backed by DataStore (async I/O);
 *   PBKDF2 derivation is also CPU-heavy by design (310,000 iterations) — callers should invoke
 *   these from a background dispatcher (e.g. Dispatchers.Default), never the main thread.
 */
class KeyLifecycleOrchestrator(
    private val keyManager: KeyManager,
    private val recoveryKeyManager: RecoveryKeyManager,
    private val cryptoManager: CryptoManager,
    private val keyMaterialStore: KeyMaterialStore
) {

    suspend fun onFirstLaunch(pin: CharArray, requireBiometric: Boolean): Result<Unit> = runCatching {
        val appEncryptionKey = SymmetricKeyGenerator.generateAesKey()
        val searchHmacKey = SymmetricKeyGenerator.generateHmacKey()
        val idIndexKey = SymmetricKeyGenerator.generateHmacKey()

        keyManager.generateDeviceMasterKey(requireUserAuthentication = requireBiometric)
        val localWrapped = keyManager.wrapLocal(appEncryptionKey)

        val salt = recoveryKeyManager.generateSalt()
        val iterations = Pbkdf2KeyDeriver.DEFAULT_ITERATIONS
        val recoveryWrapped = recoveryKeyManager.createRecoveryWrap(appEncryptionKey, pin, salt, iterations)

        val encryptedSearchKey = AesGcmCipher.encrypt(appEncryptionKey, searchHmacKey.encoded)
        val encryptedIdIndexKey = AesGcmCipher.encrypt(appEncryptionKey, idIndexKey.encoded)
        val verification = PinVerificationHasher.create(pin)

        keyMaterialStore.update {
            KeyMaterialSnapshot(
                localWrappedAppKey = localWrapped,
                recoveryWrappedAppKey = recoveryWrapped,
                recoverySalt = salt,
                recoveryKdfIterations = iterations,
                encryptedSearchHmacKey = encryptedSearchKey,
                encryptedIdIndexKey = encryptedIdIndexKey,
                pinVerificationHash = verification.hash,
                pinVerificationSalt = verification.salt,
                recoveryFailedAttemptCount = 0,
                recoveryLastFailureAt = null
            )
        }

        cryptoManager.loadKeys(appEncryptionKey, searchHmacKey, idIndexKey)
    }

    /** Requires an already-UNLOCKED session (cryptoManager holds the current App Encryption Key). */
    suspend fun onPinChange(oldPin: CharArray, newPin: CharArray): Result<Unit> {
        val current = keyMaterialStore.snapshot()
        val storedHash = current.pinVerificationHash
        val storedSalt = current.pinVerificationSalt
            ?: return Result.failure(KeyLifecycleError.NotInitialized)
        if (storedHash == null) return Result.failure(KeyLifecycleError.NotInitialized)

        val oldPinValid = PinVerificationHasher.matches(
            oldPin, PinVerificationHasher.Verification(storedHash, storedSalt)
        )
        if (!oldPinValid) return Result.failure(KeyLifecycleError.OldPinIncorrect)

        return runCatching {
            val newSalt = recoveryKeyManager.generateSalt()
            val iterations = Pbkdf2KeyDeriver.DEFAULT_ITERATIONS
            val newRecoveryWrapped = cryptoManager.useAppEncryptionKey { appKey ->
                recoveryKeyManager.createRecoveryWrap(appKey, newPin, newSalt, iterations)
            }
            val newVerification = PinVerificationHasher.create(newPin)

            keyMaterialStore.update {
                it.copy(
                    recoveryWrappedAppKey = newRecoveryWrapped,
                    recoverySalt = newSalt,
                    recoveryKdfIterations = iterations,
                    pinVerificationHash = newVerification.hash,
                    pinVerificationSalt = newVerification.salt
                    // App Encryption Key itself is NOT re-encrypted (spec 5.4) — only the
                    // Recovery Wrapped Key is regenerated under the new PIN.
                )
            }
        }
    }

    /**
     * Recovery step 1 of 2 (spec 5.4 Restore sequence). Does not touch [cryptoManager] or
     * regenerate the Local Wrapped Key yet — the caller must first run a decrypt-test against
     * a real Credential (AC-BACKUP-03) and only then call [completeRestore].
     */
    suspend fun beginRestore(pin: CharArray): Result<RecoveredKeys> {
        val current = keyMaterialStore.snapshot()
        val recoveryWrapped = current.recoveryWrappedAppKey
        val salt = current.recoverySalt
        val iterations = current.recoveryKdfIterations
        val encryptedSearchKey = current.encryptedSearchHmacKey
        val encryptedIdIndexKey = current.encryptedIdIndexKey
        if (recoveryWrapped == null || salt == null || iterations == null ||
            encryptedSearchKey == null || encryptedIdIndexKey == null
        ) {
            return Result.failure(KeyLifecycleError.NotInitialized)
        }

        val unwrapResult = recoveryKeyManager.unwrapRecovery(
            wrapped = recoveryWrapped,
            salt = salt,
            pin = pin,
            iterations = iterations,
            consecutiveFailedAttempts = current.recoveryFailedAttemptCount,
            lastFailureAt = current.recoveryLastFailureAt
        )

        if (unwrapResult.isSuccess) {
            keyMaterialStore.update { it.copy(recoveryFailedAttemptCount = 0, recoveryLastFailureAt = null) }
        } else if (unwrapResult.exceptionOrNull() is RecoveryUnwrapError.PinMismatch) {
            keyMaterialStore.update {
                it.copy(
                    recoveryFailedAttemptCount = it.recoveryFailedAttemptCount + 1,
                    recoveryLastFailureAt = Instant.now()
                )
            }
        }

        return unwrapResult.mapCatching { appEncryptionKey ->
            val searchHmacKey = SecretKeySpec(
                AesGcmCipher.decrypt(appEncryptionKey, encryptedSearchKey),
                "HmacSHA256"
            )
            val idIndexKey = SecretKeySpec(
                AesGcmCipher.decrypt(appEncryptionKey, encryptedIdIndexKey),
                "HmacSHA256"
            )
            RecoveredKeys(appEncryptionKey, searchHmacKey, idIndexKey)
        }
    }

    /** Recovery step 2 of 2 — call only after the caller's own decrypt-test has passed. */
    suspend fun completeRestore(recovered: RecoveredKeys, requireBiometric: Boolean): EncryptedBlob {
        val newLocalWrapped = keyManager.regenerateDeviceMasterKeyAndRewrap(
            recovered.appEncryptionKey, requireBiometric
        )
        keyMaterialStore.update { it.copy(localWrappedAppKey = newLocalWrapped) }
        cryptoManager.loadKeys(recovered.appEncryptionKey, recovered.searchHmacKey, recovered.idIndexKey)
        return newLocalWrapped
    }

    /**
     * Everyday "fast path" unlock (spec 5.1/5.3) split in two phases so the Cipher can be
     * handed to BiometricPrompt as a CryptoObject *before* authentication (spec 5.2: local
     * unlock deliberately relies on Android Keystore's own user-authentication condition —
     * BiometricPrompt configured for BIOMETRIC_STRONG or DEVICE_CREDENTIAL — to reuse the
     * device's own hardware-backed lockout, rather than the app's slow PBKDF2-gated PIN).
     */
    suspend fun prepareLocalUnlock(): Result<Cipher> = runCatching {
        val wrapped = keyMaterialStore.snapshot().localWrappedAppKey
            ?: throw KeyLifecycleError.NotInitialized
        keyManager.createDecryptCipher(wrapped)
    }

    /** Call with the same (now-authenticated) Cipher after BiometricPrompt succeeds. */
    suspend fun completeLocalUnlock(cipher: Cipher): Result<Unit> = runCatching {
        val snapshot = keyMaterialStore.snapshot()
        val wrapped = snapshot.localWrappedAppKey ?: throw KeyLifecycleError.NotInitialized
        val encryptedSearchKey = snapshot.encryptedSearchHmacKey ?: throw KeyLifecycleError.NotInitialized
        val encryptedIdIndexKey = snapshot.encryptedIdIndexKey ?: throw KeyLifecycleError.NotInitialized

        val appEncryptionKey = keyManager.finishUnwrapLocal(cipher, wrapped)
        val searchHmacKey = SecretKeySpec(AesGcmCipher.decrypt(appEncryptionKey, encryptedSearchKey), "HmacSHA256")
        val idIndexKey = SecretKeySpec(AesGcmCipher.decrypt(appEncryptionKey, encryptedIdIndexKey), "HmacSHA256")
        cryptoManager.loadKeys(appEncryptionKey, searchHmacKey, idIndexKey)
    }

    /**
     * Settings' Authentication toggle (spec 13.2 Settings > Authentication) — regenerates the
     * Device Master Key with a new `requireUserAuthentication` value and re-wraps the (already
     * in-memory, session-unlocked) App Encryption Key under it. Requires an UNLOCKED session.
     */
    suspend fun updateBiometricPreference(requireBiometric: Boolean): Result<Unit> = runCatching {
        val newLocalWrapped = cryptoManager.useAppEncryptionKey { appKey ->
            keyManager.regenerateDeviceMasterKeyAndRewrap(appKey, requireBiometric)
        }
        keyMaterialStore.update { it.copy(localWrappedAppKey = newLocalWrapped) }
    }
}
