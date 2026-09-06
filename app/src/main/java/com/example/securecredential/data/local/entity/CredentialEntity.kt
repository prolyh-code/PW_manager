package com.example.securecredential.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Spec section 6.1. No plaintext Credential field exists here (invariant #2). */
@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey val credentialId: String,
    val encryptedPayload: ByteArray,
    val nonce: ByteArray,
    val createdAt: Long,
    val updatedAt: Long
)
