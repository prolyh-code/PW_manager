package com.example.securecredential.data.local.database

import androidx.room.withTransaction
import com.example.securecredential.data.local.dao.CredentialDao
import com.example.securecredential.data.local.dao.SearchIndexDao
import com.example.securecredential.data.local.dao.UsernameIndexDao
import com.example.securecredential.data.local.entity.CredentialEntity
import com.example.securecredential.data.local.entity.SearchIndexEntity
import com.example.securecredential.data.local.entity.UsernameIndexEntity
import javax.inject.Inject

/**
 * Composite write operations spanning credentials + both index tables inside a single Room
 * transaction (spec 6.3 — one failure rolls back everything, preventing orphan indexes). A
 * plain class rather than a `@Dao` because Room's `@Transaction` on a DAO method only wraps
 * queries within that one DAO; spanning three DAOs needs `RoomDatabase.withTransaction`.
 */
class CredentialTransactionRunner @Inject constructor(
    private val database: SecureVaultDatabase,
    private val credentialDao: CredentialDao,
    private val searchIndexDao: SearchIndexDao,
    private val usernameIndexDao: UsernameIndexDao
) {
    suspend fun insertWithIndexes(
        credential: CredentialEntity,
        searchIndexes: List<SearchIndexEntity>,
        usernameIndex: UsernameIndexEntity
    ) = database.withTransaction {
        credentialDao.insert(credential)
        searchIndexes.forEach { searchIndexDao.insert(it) }
        usernameIndexDao.insert(usernameIndex)
    }

    suspend fun updateWithIndexes(
        credential: CredentialEntity,
        searchIndexes: List<SearchIndexEntity>,
        usernameIndex: UsernameIndexEntity
    ) = database.withTransaction {
        credentialDao.update(credential)
        searchIndexDao.deleteByCredentialId(credential.credentialId)
        usernameIndexDao.deleteByCredentialId(credential.credentialId)
        searchIndexes.forEach { searchIndexDao.insert(it) }
        usernameIndexDao.insert(usernameIndex)
    }

    suspend fun deleteWithIndexes(credential: CredentialEntity) = database.withTransaction {
        searchIndexDao.deleteByCredentialId(credential.credentialId)
        usernameIndexDao.deleteByCredentialId(credential.credentialId)
        credentialDao.delete(credential)
    }
}
