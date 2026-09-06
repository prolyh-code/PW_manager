package com.example.securecredential.data.repository

import kotlinx.serialization.json.Json

private val credentialPayloadJson = Json { ignoreUnknownKeys = true }

internal fun CredentialPayload.encodeToBytes(): ByteArray =
    credentialPayloadJson.encodeToString(CredentialPayload.serializer(), this).toByteArray(Charsets.UTF_8)

internal fun decodeCredentialPayload(bytes: ByteArray): CredentialPayload =
    credentialPayloadJson.decodeFromString(CredentialPayload.serializer(), String(bytes, Charsets.UTF_8))
