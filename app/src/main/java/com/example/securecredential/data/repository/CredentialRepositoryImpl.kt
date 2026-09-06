package com.example.securecredential.data.repository

import com.example.securecredential.core.crypto.EncryptedBlob
import com.example.securecredential.core.normalization.DomainNormalizer
import com.example.securecredential.data.local.dao.CredentialDao
import com.example.securecredential.data.local.database.CredentialTransactionRunner
import com.example.securecredential.data.local.dao.SearchIndexDao
import com.example.securecredential.data.local.dao.UsernameIndexDao
import com.example.securecredential.data.local.entity.CredentialEntity
import com.example.securecredential.data.local.entity.SearchIndexEntity
import com.example.securecredential.data.local.entity.SearchType
import com.example.securecredential.data.local.entity.UsernameIndexEntity
import com.example.securecredential.data.security.CryptoManager
import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.model.SearchQuery
import com.example.securecredential.domain.repository.CredentialRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CredentialRepositoryImpl @Inject constructor(
    private val credentialDao: CredentialDao,
    private val searchIndexDao: SearchIndexDao,
    private val usernameIndexDao: UsernameIndexDao,
    private val transactionRunner: CredentialTransactionRunner,
    private val cryptoManager: CryptoManager,
    private val passwordReuseChecker: PasswordReuseChecker
) : CredentialRepository {

    override suspend fun create(credential: Credential): Result<String> = runCatching {
        val now = Instant.now()
        val toStore = credential.copy(credentialId = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
        val rows = buildRows(toStore)
        transactionRunner.insertWithIndexes(rows.entity, rows.searchIndexes, rows.usernameIndex)
        toStore.credentialId
    }

    override suspend fun update(credential: Credential): Result<Unit> = runCatching {
        val toStore = credential.copy(updatedAt = Instant.now())
        val rows = buildRows(toStore)
        transactionRunner.updateWithIndexes(rows.entity, rows.searchIndexes, rows.usernameIndex)
    }

    override suspend fun delete(credentialId: String): Result<Unit> = runCatching {
        val entity = credentialDao.findById(credentialId) ?: return@runCatching
        transactionRunner.deleteWithIndexes(entity)
    }

    override suspend fun get(credentialId: String): Result<Credential> = runCatching {
        val entity = credentialDao.findById(credentialId)
            ?: throw NoSuchElementException("Credential not found: $credentialId")
        entity.toDomain()
    }

    override suspend fun search(query: SearchQuery): Result<List<CredentialSummary>> = runCatching {
        val (namespace, value) = when (query) {
            is SearchQuery.Domain -> "domain" to DomainNormalizer.normalizeDomain(query.value)
            is SearchQuery.Url -> "domain" to DomainNormalizer.extractDomainFromUrl(query.value)
            is SearchQuery.Text -> "service" to query.value.trim().lowercase()
        }
        val token = cryptoManager.searchToken(namespace, value)
        val ids = searchIndexDao.findCredentialIds(token)
        ids.mapNotNull { credentialDao.findById(it) }.map { it.toDomain().toSummary() }
    }

    override suspend fun checkPasswordReuse(password: String): Result<Boolean> = runCatching {
        passwordReuseChecker.isReused(password)
    }

    override suspend fun listAll(): Result<List<CredentialSummary>> = runCatching {
        credentialDao.getAll().map { it.toDomain().toSummary() }
    }

    override suspend fun findByUsername(username: String): Result<List<CredentialSummary>> = runCatching {
        val token = cryptoManager.usernameToken(username.trim().lowercase())
        val ids = usernameIndexDao.findCredentialIds(token)
        ids.mapNotNull { credentialDao.findById(it) }.map { it.toDomain().toSummary() }
    }

    private fun CredentialEntity.toDomain(): Credential {
        val plaintext = cryptoManager.decryptCredential(EncryptedBlob(encryptedPayload, nonce))
        val payload = decodeCredentialPayload(plaintext)
        return Credential(
            credentialId = credentialId,
            serviceName = payload.serviceName,
            url = payload.url,
            domain = payload.domain,
            username = payload.username,
            password = payload.password,
            usernameMask = payload.usernameMask,
            passwordMask = payload.passwordMask,
            category = payload.category,
            memo = payload.memo,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt)
        )
    }

    private fun Credential.toSummary() = CredentialSummary(
        credentialId = credentialId,
        serviceName = serviceName,
        domain = domain,
        usernameMask = usernameMask,
        passwordMask = passwordMask
    )

    private data class Rows(
        val entity: CredentialEntity,
        val searchIndexes: List<SearchIndexEntity>,
        val usernameIndex: UsernameIndexEntity
    )

    private fun buildRows(credential: Credential): Rows {
        val payload = CredentialPayload(
            serviceName = credential.serviceName,
            url = credential.url,
            domain = credential.domain,
            username = credential.username,
            password = credential.password,
            usernameMask = credential.usernameMask,
            passwordMask = credential.passwordMask,
            category = credential.category,
            memo = credential.memo
        )
        val blob = cryptoManager.encryptCredential(payload.encodeToBytes())
        val entity = CredentialEntity(
            credentialId = credential.credentialId,
            encryptedPayload = blob.ciphertext,
            nonce = blob.nonce,
            createdAt = credential.createdAt.toEpochMilli(),
            updatedAt = credential.updatedAt.toEpochMilli()
        )

        val searchIndexes = buildList {
            val normalizedService = credential.serviceName.trim().lowercase()
            if (normalizedService.isNotEmpty()) {
                add(
                    SearchIndexEntity(
                        indexId = UUID.randomUUID().toString(),
                        tokenHash = cryptoManager.searchToken("service", normalizedService),
                        credentialId = credential.credentialId,
                        searchType = SearchType.SERVICE
                    )
                )
            }
            val normalizedDomain = credential.domain?.let { DomainNormalizer.normalizeDomain(it) }
            if (!normalizedDomain.isNullOrEmpty()) {
                add(
                    SearchIndexEntity(
                        indexId = UUID.randomUUID().toString(),
                        tokenHash = cryptoManager.searchToken("domain", normalizedDomain),
                        credentialId = credential.credentialId,
                        searchType = SearchType.DOMAIN
                    )
                )
            }
        }

        val usernameIndex = UsernameIndexEntity(
            indexId = UUID.randomUUID().toString(),
            tokenHash = cryptoManager.usernameToken(credential.username.trim().lowercase()),
            credentialId = credential.credentialId
        )

        return Rows(entity, searchIndexes, usernameIndex)
    }
}
