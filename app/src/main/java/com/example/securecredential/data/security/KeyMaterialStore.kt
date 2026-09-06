package com.example.securecredential.data.security

import com.example.securecredential.core.crypto.EncryptedBlob
import java.time.Instant

/** Spec 5.1, 5.2 — wrapped key material and PIN/Recovery metadata, as one immutable snapshot. */
data class KeyMaterialSnapshot(
    val localWrappedAppKey: EncryptedBlob? = null,
    val recoveryWrappedAppKey: EncryptedBlob? = null,
    val recoverySalt: ByteArray? = null,
    val recoveryKdfIterations: Int? = null,
    val encryptedSearchHmacKey: EncryptedBlob? = null,
    val encryptedIdIndexKey: EncryptedBlob? = null,
    val pinVerificationHash: ByteArray? = null,
    val pinVerificationSalt: ByteArray? = null,
    val recoveryFailedAttemptCount: Int = 0,
    val recoveryLastFailureAt: Instant? = null
)

/**
 * Persistence port for [KeyMaterialSnapshot] (spec 5.1/5.2). suspend-based because the real
 * implementation ([com.example.securecredential.data.preferences.DataStoreKeyMaterialStore],
 * Milestone C) is backed by Jetpack DataStore, which is inherently async I/O — there is no
 * synchronous way to read/write it, so this port can't expose plain `var` properties.
 */
interface KeyMaterialStore {
    suspend fun snapshot(): KeyMaterialSnapshot
    suspend fun update(mutate: (KeyMaterialSnapshot) -> KeyMaterialSnapshot)
}

/**
 * In-memory [KeyMaterialStore] for tests and for driving the crypto core before Milestone C's
 * real implementation existed. Not wired via Hilt in production.
 */
class InMemoryKeyMaterialStore : KeyMaterialStore {
    private var current = KeyMaterialSnapshot()

    override suspend fun snapshot(): KeyMaterialSnapshot = current

    override suspend fun update(mutate: (KeyMaterialSnapshot) -> KeyMaterialSnapshot) {
        current = mutate(current)
    }
}
