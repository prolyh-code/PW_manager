package com.example.securecredential.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.securecredential.core.crypto.Pbkdf2KeyDeriver
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [KeyLifecycleOrchestrator] against the real Keystore-backed [AndroidKeystoreKeyManager],
 * simulating the spec 5.4 Restore sequence: a "new device" is stood in for by deleting only the
 * Keystore alias (keyMaterialStore, the stand-in for the Backup payload, is left untouched).
 * This is the closest automatable approximation of AC-BACKUP-03 available before Milestone B's
 * Room-backed Repository exists to run a real Credential decrypt-test against.
 */
@RunWith(AndroidJUnit4::class)
class KeyLifecycleOrchestratorInstrumentedTest {

    private lateinit var keyManager: AndroidKeystoreKeyManager
    private lateinit var recoveryKeyManager: RecoveryKeyManager
    private lateinit var cryptoManager: CryptoManagerImpl
    private lateinit var keyMaterialStore: InMemoryKeyMaterialStore
    private lateinit var orchestrator: KeyLifecycleOrchestrator

    @Before
    fun setUp() {
        keyManager = AndroidKeystoreKeyManager()
        keyManager.deleteDeviceMasterKey()
        recoveryKeyManager = RecoveryKeyManager(Pbkdf2KeyDeriver())
        cryptoManager = CryptoManagerImpl()
        keyMaterialStore = InMemoryKeyMaterialStore()
        orchestrator = KeyLifecycleOrchestrator(keyManager, recoveryKeyManager, cryptoManager, keyMaterialStore)
    }

    @After
    fun tearDown() {
        keyManager.deleteDeviceMasterKey()
    }

    @Test
    fun restoreSequence_recoversKeysAndDecryptsDataFromBeforeTheSimulatedNewDevice() = runTest {
        val pin = "MySecurePin123".toCharArray()
        assertTrue(orchestrator.onFirstLaunch(pin, requireBiometric = false).isSuccess)

        val sampleBlob = cryptoManager.encryptCredential("sample-credential-password".toByteArray())

        // Simulate "new device": the Keystore-backed Device Master Key is gone, but the
        // Backup payload (here: keyMaterialStore, kept in memory) survived the transfer.
        keyManager.deleteDeviceMasterKey()
        cryptoManager.clearKeys()

        val recovered = orchestrator.beginRestore(pin)
        assertTrue("PIN recovery should succeed with the original PIN", recovered.isSuccess)

        // Stand-in for AC-BACKUP-03's decrypt-test: verify the recovered key actually
        // decrypts data that was encrypted before the simulated device change.
        val recoveredKeys = recovered.getOrThrow()
        val decryptTestPassed = try {
            com.example.securecredential.core.crypto.AesGcmCipher.decrypt(recoveredKeys.appEncryptionKey, sampleBlob)
            true
        } catch (e: Exception) {
            false
        }
        assertTrue("decrypt-test against pre-restore data must pass before completing restore", decryptTestPassed)

        orchestrator.completeRestore(recoveredKeys, requireBiometric = false)

        assertTrue(keyManager.deviceMasterKeyExists())
        assertTrue(cryptoManager.isUnlocked)
        assertArrayEquals(
            "sample-credential-password".toByteArray(),
            cryptoManager.decryptCredential(sampleBlob)
        )
    }

    @Test
    fun restoreSequence_withWrongPin_doesNotRegenerateDeviceKeyAndLeavesSessionLocked() = runTest {
        val pin = "MySecurePin123".toCharArray()
        orchestrator.onFirstLaunch(pin, requireBiometric = false)
        keyManager.deleteDeviceMasterKey()
        cryptoManager.clearKeys()

        val result = orchestrator.beginRestore("WrongPin000".toCharArray())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RecoveryUnwrapError.PinMismatch)
        assertTrue(
            "a failed restore must not silently leave a usable Device Master Key behind",
            !keyManager.deviceMasterKeyExists()
        )
        assertTrue(!cryptoManager.isUnlocked)
    }

    @Test
    fun localUnlock_prepareThenComplete_unlocksCryptoManagerWithTheOriginalKeys() = runTest {
        val pin = "MySecurePin123".toCharArray()
        orchestrator.onFirstLaunch(pin, requireBiometric = false)
        val sampleBlob = cryptoManager.encryptCredential("local-unlock-sample".toByteArray())
        cryptoManager.clearKeys() // simulate re-locking; the wrapped keys on disk are untouched

        val cipher = orchestrator.prepareLocalUnlock().getOrThrow()
        val completeResult = orchestrator.completeLocalUnlock(cipher)

        assertTrue(completeResult.isSuccess)
        assertTrue(cryptoManager.isUnlocked)
        assertArrayEquals("local-unlock-sample".toByteArray(), cryptoManager.decryptCredential(sampleBlob))
    }
}
