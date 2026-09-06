package com.example.securecredential.data.repository

import com.example.securecredential.core.crypto.EncryptedBlob
import com.example.securecredential.data.local.dao.CredentialDao
import com.example.securecredential.data.security.CryptoManager
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Spec section 7 abstraction seam — swap in a security-reviewed Keyed Index implementation
 * later (P1) if data scale grows; MVP's [DecryptAllPasswordReuseChecker] is the only impl.
 */
interface PasswordReuseChecker {
    suspend fun isReused(candidatePassword: String): Boolean
}

/**
 * Decrypts every stored Credential at check time and compares in constant time
 * (MessageDigest.isEqual) to mitigate timing attacks (spec 7). Deliberately has no HMAC
 * equality index — that design was rejected for low-entropy-password dictionary-attack risk.
 * O(n) full-table decrypt on every create/update; accepted for MVP scale (tens-hundreds of
 * rows) since it only runs on writes, never on search.
 */
class DecryptAllPasswordReuseChecker @Inject constructor(
    private val credentialDao: CredentialDao,
    private val cryptoManager: CryptoManager
) : PasswordReuseChecker {

    override suspend fun isReused(candidatePassword: String): Boolean {
        val candidateBytes = candidatePassword.toByteArray(Charsets.UTF_8)
        for (entity in credentialDao.getAll()) {
            val plaintext = cryptoManager.decryptCredential(EncryptedBlob(entity.encryptedPayload, entity.nonce))
            val storedPassword = decodeCredentialPayload(plaintext).password.toByteArray(Charsets.UTF_8)
            if (MessageDigest.isEqual(candidateBytes, storedPassword)) {
                return true
            }
        }
        return false
    }
}
