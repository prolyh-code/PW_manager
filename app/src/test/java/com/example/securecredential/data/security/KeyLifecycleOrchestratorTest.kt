package com.example.securecredential.data.security

import com.example.securecredential.core.crypto.Pbkdf2KeyDeriver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeyLifecycleOrchestratorTest {

    private lateinit var keyManager: FakeKeyManager
    private lateinit var recoveryKeyManager: RecoveryKeyManager
    private lateinit var cryptoManager: CryptoManagerImpl
    private lateinit var keyMaterialStore: InMemoryKeyMaterialStore
    private lateinit var orchestrator: KeyLifecycleOrchestrator

    @Before
    fun setUp() {
        keyManager = FakeKeyManager()
        recoveryKeyManager = RecoveryKeyManager(Pbkdf2KeyDeriver())
        cryptoManager = CryptoManagerImpl()
        keyMaterialStore = InMemoryKeyMaterialStore()
        orchestrator = KeyLifecycleOrchestrator(keyManager, recoveryKeyManager, cryptoManager, keyMaterialStore)
    }

    @Test
    fun `first launch produces a fully READY state`() = runTest {
        val result = orchestrator.onFirstLaunch("MySecurePin123".toCharArray(), requireBiometric = false)

        assertTrue(result.isSuccess)
        assertTrue(cryptoManager.isUnlocked)
        val snapshot = keyMaterialStore.snapshot()
        assertNotNull(snapshot.localWrappedAppKey)
        assertNotNull(snapshot.recoveryWrappedAppKey)
        assertNotNull(snapshot.recoverySalt)
        assertNotNull(snapshot.encryptedSearchHmacKey)
        assertNotNull(snapshot.encryptedIdIndexKey)
        assertNotNull(snapshot.pinVerificationHash)
        assertEquals(0, snapshot.recoveryFailedAttemptCount)
    }

    @Test
    fun `first launch lets CryptoManager encrypt and decrypt round trip`() = runTest {
        orchestrator.onFirstLaunch("MySecurePin123".toCharArray(), requireBiometric = false)

        val blob = cryptoManager.encryptCredential("hunter2".toByteArray())
        val decrypted = cryptoManager.decryptCredential(blob)

        assertArrayEquals("hunter2".toByteArray(), decrypted)
    }

    @Test
    fun `pin change with correct old pin succeeds and rotates recovery wrap`() = runTest {
        orchestrator.onFirstLaunch("OldPin12345".toCharArray(), requireBiometric = false)
        val before = keyMaterialStore.snapshot()

        val result = orchestrator.onPinChange("OldPin12345".toCharArray(), "NewPin67890".toCharArray())

        assertTrue(result.isSuccess)
        val after = keyMaterialStore.snapshot()
        assertFalse(before.recoveryWrappedAppKey!!.ciphertext.contentEquals(after.recoveryWrappedAppKey!!.ciphertext))
        assertFalse(before.recoverySalt!!.contentEquals(after.recoverySalt!!))
    }

    @Test
    fun `pin change with wrong old pin fails and does not touch recovery wrap`() = runTest {
        orchestrator.onFirstLaunch("OldPin12345".toCharArray(), requireBiometric = false)
        val before = keyMaterialStore.snapshot()

        val result = orchestrator.onPinChange("WrongOldPin99".toCharArray(), "NewPin67890".toCharArray())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is KeyLifecycleError.OldPinIncorrect)
        val after = keyMaterialStore.snapshot()
        assertArrayEquals(before.recoveryWrappedAppKey!!.ciphertext, after.recoveryWrappedAppKey!!.ciphertext)
    }

    @Test
    fun `pin change does not re-encrypt the app encryption key (spec 5_4)`() = runTest {
        orchestrator.onFirstLaunch("OldPin12345".toCharArray(), requireBiometric = false)
        val blob = cryptoManager.encryptCredential("unchanged".toByteArray())

        orchestrator.onPinChange("OldPin12345".toCharArray(), "NewPin67890".toCharArray())

        // Same in-memory App Encryption Key still decrypts data encrypted before the PIN change.
        assertArrayEquals("unchanged".toByteArray(), cryptoManager.decryptCredential(blob))
    }

    @Test
    fun `restore with correct pin recovers keys usable to decrypt prior data`() = runTest {
        orchestrator.onFirstLaunch("MySecurePin123".toCharArray(), requireBiometric = false)
        val blobFromBeforeRestore = cryptoManager.encryptCredential("still here".toByteArray())
        cryptoManager.clearKeys() // simulate "new device": in-memory session lost, but store (the "backup") remains

        val recoverResult = orchestrator.beginRestore("MySecurePin123".toCharArray())
        assertTrue(recoverResult.isSuccess)

        val newLocalWrapped = orchestrator.completeRestore(recoverResult.getOrThrow(), requireBiometric = false)

        assertNotNull(newLocalWrapped)
        assertTrue(cryptoManager.isUnlocked)
        assertArrayEquals("still here".toByteArray(), cryptoManager.decryptCredential(blobFromBeforeRestore))
    }

    @Test
    fun `restore with wrong pin fails and increments the failure counter`() = runTest {
        orchestrator.onFirstLaunch("MySecurePin123".toCharArray(), requireBiometric = false)
        cryptoManager.clearKeys()

        val result = orchestrator.beginRestore("WrongPin000".toCharArray())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RecoveryUnwrapError.PinMismatch)
        val snapshot = keyMaterialStore.snapshot()
        assertEquals(1, snapshot.recoveryFailedAttemptCount)
        assertNotNull(snapshot.recoveryLastFailureAt)
        assertFalse(cryptoManager.isUnlocked)
    }

    @Test
    fun `restore before first launch fails with NotInitialized`() = runTest {
        val result = orchestrator.beginRestore("AnyPin12345".toCharArray())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is KeyLifecycleError.NotInitialized)
    }

    @Test
    fun `updateBiometricPreference re-wraps the local key without disturbing decrypted data`() = runTest {
        orchestrator.onFirstLaunch("MySecurePin123".toCharArray(), requireBiometric = false)
        val before = keyMaterialStore.snapshot().localWrappedAppKey
        val blob = cryptoManager.encryptCredential("unchanged-by-toggle".toByteArray())

        val result = orchestrator.updateBiometricPreference(requireBiometric = true)

        assertTrue(result.isSuccess)
        val after = keyMaterialStore.snapshot().localWrappedAppKey
        assertFalse(before!!.ciphertext.contentEquals(after!!.ciphertext))
        assertArrayEquals("unchanged-by-toggle".toByteArray(), cryptoManager.decryptCredential(blob))
    }

    private fun assertFalse(condition: Boolean) = assertTrue(!condition)
}
