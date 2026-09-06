package com.example.securecredential.core.crypto

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AesGcmCipherTest {

    private fun randomKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun `encrypt then decrypt returns original plaintext`() {
        val key = randomKey()
        val plaintext = "correct horse battery staple".toByteArray()

        val blob = AesGcmCipher.encrypt(key, plaintext)
        val decrypted = AesGcmCipher.decrypt(key, blob)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `same plaintext encrypted twice produces different ciphertext and nonce`() {
        val key = randomKey()
        val plaintext = "same input".toByteArray()

        val blobA = AesGcmCipher.encrypt(key, plaintext)
        val blobB = AesGcmCipher.encrypt(key, plaintext)

        assert(!blobA.nonce.contentEquals(blobB.nonce)) { "nonces must not repeat" }
        assert(!blobA.ciphertext.contentEquals(blobB.ciphertext)) { "ciphertext must differ when nonce differs" }
    }

    @Test
    fun `tampered ciphertext fails integrity check`() {
        val key = randomKey()
        val blob = AesGcmCipher.encrypt(key, "sensitive".toByteArray())
        val tampered = blob.copy(ciphertext = blob.ciphertext.also { it[0] = it[0].inc() })

        assertThrows(AeadIntegrityException::class.java) {
            AesGcmCipher.decrypt(key, tampered)
        }
    }

    @Test
    fun `tampered nonce fails integrity check`() {
        val key = randomKey()
        val blob = AesGcmCipher.encrypt(key, "sensitive".toByteArray())
        val tampered = blob.copy(nonce = blob.nonce.also { it[0] = it[0].inc() })

        assertThrows(AeadIntegrityException::class.java) {
            AesGcmCipher.decrypt(key, tampered)
        }
    }

    @Test
    fun `decrypting with wrong key fails integrity check`() {
        val blob = AesGcmCipher.encrypt(randomKey(), "sensitive".toByteArray())

        assertThrows(AeadIntegrityException::class.java) {
            AesGcmCipher.decrypt(randomKey(), blob)
        }
    }
}
