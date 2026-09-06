package com.example.securecredential.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.securecredential.core.crypto.SymmetricKeyGenerator
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real "AndroidKeyStore" provider — cannot run on the plain JVM unit test
 * runner (Robolectric does not faithfully emulate hardware-backed Keystore behavior; spec's
 * own plan file calls this out explicitly).
 */
@RunWith(AndroidJUnit4::class)
class AndroidKeystoreKeyManagerTest {

    private lateinit var keyManager: AndroidKeystoreKeyManager

    @Before
    fun setUp() {
        keyManager = AndroidKeystoreKeyManager()
        keyManager.deleteDeviceMasterKey() // clean slate: tests may run against a reused device/emulator
    }

    @After
    fun tearDown() {
        keyManager.deleteDeviceMasterKey()
    }

    @Test
    fun deviceMasterKeyExists_isFalse_beforeGeneration() {
        assertFalse(keyManager.deviceMasterKeyExists())
    }

    @Test
    fun generate_thenWrapThenUnwrap_roundTripsTheAppEncryptionKey() {
        keyManager.generateDeviceMasterKey(requireUserAuthentication = false)
        assertTrue(keyManager.deviceMasterKeyExists())

        val appEncryptionKey = SymmetricKeyGenerator.generateAesKey()
        val wrapped = keyManager.wrapLocal(appEncryptionKey)
        val unwrapped = keyManager.unwrapLocal(wrapped)

        assertArrayEquals(appEncryptionKey.encoded, unwrapped.encoded)
    }

    @Test
    fun unwrap_withTamperedCiphertext_throwsIntegrityException() {
        keyManager.generateDeviceMasterKey(requireUserAuthentication = false)
        val appEncryptionKey = SymmetricKeyGenerator.generateAesKey()
        val wrapped = keyManager.wrapLocal(appEncryptionKey)
        val tampered = wrapped.copy(ciphertext = wrapped.ciphertext.also { it[0] = it[0].inc() })

        // AndroidKeyStore's Cipher throws AEADBadTagException same as the software path;
        // finishUnwrapLocal does not currently wrap it, so assert the underlying JCA type.
        assertThrows(javax.crypto.AEADBadTagException::class.java) {
            keyManager.unwrapLocal(tampered)
        }
    }

    @Test
    fun regenerateDeviceMasterKeyAndRewrap_producesAKeyUsableForTheSameAppEncryptionKey() {
        keyManager.generateDeviceMasterKey(requireUserAuthentication = false)
        val appEncryptionKey = SymmetricKeyGenerator.generateAesKey()
        keyManager.wrapLocal(appEncryptionKey)

        val newWrapped = keyManager.regenerateDeviceMasterKeyAndRewrap(
            appEncryptionKey, requireUserAuthentication = false
        )
        val unwrapped = keyManager.unwrapLocal(newWrapped)

        assertArrayEquals(appEncryptionKey.encoded, unwrapped.encoded)
    }

    @Test
    fun deleteDeviceMasterKey_removesTheAlias() {
        keyManager.generateDeviceMasterKey(requireUserAuthentication = false)
        assertTrue(keyManager.deviceMasterKeyExists())

        keyManager.deleteDeviceMasterKey()

        assertFalse(keyManager.deviceMasterKeyExists())
    }
}
