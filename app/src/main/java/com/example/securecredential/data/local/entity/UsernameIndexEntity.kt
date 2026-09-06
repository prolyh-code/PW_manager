package com.example.securecredential.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Spec section 6.1. tokenHash = HMAC(IdIndexKey, normalizedUsername) — Exact Match only.
 * IdIndexKey is a separate key from the Search HMAC Key (see CryptoManager) so this index's
 * token space is unlinkable from the domain/service search index.
 */
@Entity(tableName = "username_index", indices = [Index(value = ["tokenHash"])])
data class UsernameIndexEntity(
    @PrimaryKey val indexId: String,
    val tokenHash: ByteArray,
    val credentialId: String
)
