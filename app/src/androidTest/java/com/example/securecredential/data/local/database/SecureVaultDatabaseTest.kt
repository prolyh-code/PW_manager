package com.example.securecredential.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.securecredential.data.local.dao.CredentialDao
import com.example.securecredential.data.local.dao.SearchIndexDao
import com.example.securecredential.data.local.dao.UsernameIndexDao
import com.example.securecredential.data.local.entity.CredentialEntity
import com.example.securecredential.data.local.entity.SearchIndexEntity
import com.example.securecredential.data.local.entity.SearchType
import com.example.securecredential.data.local.entity.UsernameIndexEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureVaultDatabaseTest {

    private lateinit var database: SecureVaultDatabase
    private lateinit var credentialDao: CredentialDao
    private lateinit var searchIndexDao: SearchIndexDao
    private lateinit var usernameIndexDao: UsernameIndexDao
    private lateinit var transactionRunner: CredentialTransactionRunner

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SecureVaultDatabase::class.java
        ).allowMainThreadQueries().build()
        credentialDao = database.credentialDao()
        searchIndexDao = database.searchIndexDao()
        usernameIndexDao = database.usernameIndexDao()
        transactionRunner = CredentialTransactionRunner(database, credentialDao, searchIndexDao, usernameIndexDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // CredentialEntity is a data class over ByteArray fields, so its generated equals() compares
    // array references, not contents — assertEquals(entity, entity) can spuriously fail even
    // though toString() prints identical contents. Compare fields explicitly instead.
    private fun assertCredentialEntityEquals(expected: CredentialEntity, actual: CredentialEntity?) {
        assertTrue("expected a non-null result", actual != null)
        assertEquals(expected.credentialId, actual!!.credentialId)
        assertTrue(expected.encryptedPayload.contentEquals(actual.encryptedPayload))
        assertTrue(expected.nonce.contentEquals(actual.nonce))
        assertEquals(expected.createdAt, actual.createdAt)
        assertEquals(expected.updatedAt, actual.updatedAt)
    }

    private fun sampleCredential(id: String = "cred-1") = CredentialEntity(
        credentialId = id,
        encryptedPayload = byteArrayOf(1, 2, 3),
        nonce = byteArrayOf(4, 5, 6),
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun credentialDao_insertThenFindById_roundTrips() = kotlinx.coroutines.runBlocking {
        val entity = sampleCredential()
        credentialDao.insert(entity)

        val found = credentialDao.findById("cred-1")

        assertCredentialEntityEquals(entity, found)
    }

    @Test
    fun credentialDao_delete_removesRow() = kotlinx.coroutines.runBlocking {
        val entity = sampleCredential()
        credentialDao.insert(entity)

        credentialDao.delete(entity)

        assertNull(credentialDao.findById("cred-1"))
    }

    @Test
    fun searchIndexDao_findCredentialIds_returnsMatchingToken() = kotlinx.coroutines.runBlocking {
        val token = byteArrayOf(9, 9, 9)
        searchIndexDao.insert(
            SearchIndexEntity("idx-1", token, "cred-1", SearchType.DOMAIN)
        )

        val ids = searchIndexDao.findCredentialIds(token)

        assertEquals(listOf("cred-1"), ids)
    }

    @Test
    fun searchIndexDao_deleteByCredentialId_removesOnlyThatCredentialsRows() = kotlinx.coroutines.runBlocking {
        searchIndexDao.insert(SearchIndexEntity("idx-1", byteArrayOf(1), "cred-1", SearchType.SERVICE))
        searchIndexDao.insert(SearchIndexEntity("idx-2", byteArrayOf(2), "cred-2", SearchType.SERVICE))

        searchIndexDao.deleteByCredentialId("cred-1")

        assertTrue(searchIndexDao.findCredentialIds(byteArrayOf(1)).isEmpty())
        assertEquals(listOf("cred-2"), searchIndexDao.findCredentialIds(byteArrayOf(2)))
    }

    @Test
    fun usernameIndexDao_insertThenFind_roundTrips() = kotlinx.coroutines.runBlocking {
        val token = byteArrayOf(7, 7, 7)
        usernameIndexDao.insert(UsernameIndexEntity("uidx-1", token, "cred-1"))

        assertEquals(listOf("cred-1"), usernameIndexDao.findCredentialIds(token))
    }

    @Test
    fun transaction_insertWithIndexes_writesAllThreeTablesAtomically() = kotlinx.coroutines.runBlocking {
        val entity = sampleCredential()
        val searchIndex = SearchIndexEntity("idx-1", byteArrayOf(1), "cred-1", SearchType.SERVICE)
        val usernameIndex = UsernameIndexEntity("uidx-1", byteArrayOf(2), "cred-1")

        transactionRunner.insertWithIndexes(entity, listOf(searchIndex), usernameIndex)

        assertCredentialEntityEquals(entity, credentialDao.findById("cred-1"))
        assertEquals(listOf("cred-1"), searchIndexDao.findCredentialIds(byteArrayOf(1)))
        assertEquals(listOf("cred-1"), usernameIndexDao.findCredentialIds(byteArrayOf(2)))
    }

    @Test
    fun transaction_failurePartwayThrough_rollsBackEverything_noOrphanIndexes() = kotlinx.coroutines.runBlocking {
        val entity = sampleCredential()
        // A duplicate-primary-key insert forces a failure AFTER the credential row and the
        // search index would already be written, but BEFORE the username index — asserting
        // that Room's transaction rolls back the credential+search-index writes too (spec
        // 6.3's "one failure -> full rollback", the orphan-index guard).
        credentialDao.insert(entity) // pre-existing row makes the transaction's own insert collide

        val thrown = runCatching {
            transactionRunner.insertWithIndexes(
                entity, // will violate the PRIMARY KEY constraint (already exists)
                listOf(SearchIndexEntity("idx-1", byteArrayOf(1), "cred-1", SearchType.SERVICE)),
                UsernameIndexEntity("uidx-1", byteArrayOf(2), "cred-1")
            )
        }

        assertTrue("expected the duplicate insert to throw", thrown.isFailure)
        // No index rows should exist — the whole transaction (including the search index insert
        // that ran before the failure point) must have rolled back.
        assertTrue(searchIndexDao.findCredentialIds(byteArrayOf(1)).isEmpty())
        assertTrue(usernameIndexDao.findCredentialIds(byteArrayOf(2)).isEmpty())
    }

    @Test
    fun transaction_deleteWithIndexes_removesCredentialAndBothIndexes() = kotlinx.coroutines.runBlocking {
        val entity = sampleCredential()
        transactionRunner.insertWithIndexes(
            entity,
            listOf(SearchIndexEntity("idx-1", byteArrayOf(1), "cred-1", SearchType.SERVICE)),
            UsernameIndexEntity("uidx-1", byteArrayOf(2), "cred-1")
        )

        transactionRunner.deleteWithIndexes(entity)

        assertNull(credentialDao.findById("cred-1"))
        assertTrue(searchIndexDao.findCredentialIds(byteArrayOf(1)).isEmpty())
        assertTrue(usernameIndexDao.findCredentialIds(byteArrayOf(2)).isEmpty())
    }
}
