package com.example.securecredential.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.securecredential.core.crypto.EncryptedBlob
import com.example.securecredential.data.security.KeyMaterialSnapshot
import com.example.securecredential.data.security.KeyMaterialStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.keyMaterialDataStore by preferencesDataStore(name = DataStoreKeyMaterialStore.DATASTORE_NAME)

/**
 * DataStore-backed [KeyMaterialStore] (spec 5.1/5.2's wrapped-key/PIN/Recovery metadata).
 * Preferences DataStore has no ByteArray value type, so every binary field is Base64-encoded
 * to a String. [DATASTORE_NAME] is a named constant (not an inline string) because Milestone
 * E's data-extraction-rules.xml must reference this exact DataStore file by name.
 */
@Singleton
class DataStoreKeyMaterialStore @Inject constructor(
    @ApplicationContext private val context: Context
) : KeyMaterialStore {

    companion object {
        const val DATASTORE_NAME = "key_material"
    }

    private object Keys {
        val LOCAL_WRAPPED_CIPHERTEXT = stringPreferencesKey("local_wrapped_ciphertext")
        val LOCAL_WRAPPED_NONCE = stringPreferencesKey("local_wrapped_nonce")
        val RECOVERY_WRAPPED_CIPHERTEXT = stringPreferencesKey("recovery_wrapped_ciphertext")
        val RECOVERY_WRAPPED_NONCE = stringPreferencesKey("recovery_wrapped_nonce")
        val RECOVERY_SALT = stringPreferencesKey("recovery_salt")
        val RECOVERY_KDF_ITERATIONS = intPreferencesKey("recovery_kdf_iterations")
        val SEARCH_KEY_CIPHERTEXT = stringPreferencesKey("search_key_ciphertext")
        val SEARCH_KEY_NONCE = stringPreferencesKey("search_key_nonce")
        val ID_INDEX_KEY_CIPHERTEXT = stringPreferencesKey("id_index_key_ciphertext")
        val ID_INDEX_KEY_NONCE = stringPreferencesKey("id_index_key_nonce")
        val PIN_VERIFICATION_HASH = stringPreferencesKey("pin_verification_hash")
        val PIN_VERIFICATION_SALT = stringPreferencesKey("pin_verification_salt")
        val RECOVERY_FAILED_ATTEMPT_COUNT = intPreferencesKey("recovery_failed_attempt_count")
        val RECOVERY_LAST_FAILURE_AT_EPOCH_MILLI = longPreferencesKey("recovery_last_failure_at_epoch_milli")
    }

    override suspend fun snapshot(): KeyMaterialSnapshot =
        context.keyMaterialDataStore.data.first().toSnapshot()

    override suspend fun update(mutate: (KeyMaterialSnapshot) -> KeyMaterialSnapshot) {
        context.keyMaterialDataStore.edit { prefs ->
            val next = mutate(prefs.toSnapshot())
            prefs.writeBlob(Keys.LOCAL_WRAPPED_CIPHERTEXT, Keys.LOCAL_WRAPPED_NONCE, next.localWrappedAppKey)
            prefs.writeBlob(Keys.RECOVERY_WRAPPED_CIPHERTEXT, Keys.RECOVERY_WRAPPED_NONCE, next.recoveryWrappedAppKey)
            prefs.writeBytes(Keys.RECOVERY_SALT, next.recoverySalt)
            prefs.writeInt(Keys.RECOVERY_KDF_ITERATIONS, next.recoveryKdfIterations)
            prefs.writeBlob(Keys.SEARCH_KEY_CIPHERTEXT, Keys.SEARCH_KEY_NONCE, next.encryptedSearchHmacKey)
            prefs.writeBlob(Keys.ID_INDEX_KEY_CIPHERTEXT, Keys.ID_INDEX_KEY_NONCE, next.encryptedIdIndexKey)
            prefs.writeBytes(Keys.PIN_VERIFICATION_HASH, next.pinVerificationHash)
            prefs.writeBytes(Keys.PIN_VERIFICATION_SALT, next.pinVerificationSalt)
            prefs[Keys.RECOVERY_FAILED_ATTEMPT_COUNT] = next.recoveryFailedAttemptCount
            prefs.writeLong(Keys.RECOVERY_LAST_FAILURE_AT_EPOCH_MILLI, next.recoveryLastFailureAt?.toEpochMilli())
        }
    }

    private fun Preferences.toSnapshot() = KeyMaterialSnapshot(
        localWrappedAppKey = readBlob(Keys.LOCAL_WRAPPED_CIPHERTEXT, Keys.LOCAL_WRAPPED_NONCE),
        recoveryWrappedAppKey = readBlob(Keys.RECOVERY_WRAPPED_CIPHERTEXT, Keys.RECOVERY_WRAPPED_NONCE),
        recoverySalt = this[Keys.RECOVERY_SALT]?.let(::decode),
        recoveryKdfIterations = this[Keys.RECOVERY_KDF_ITERATIONS],
        encryptedSearchHmacKey = readBlob(Keys.SEARCH_KEY_CIPHERTEXT, Keys.SEARCH_KEY_NONCE),
        encryptedIdIndexKey = readBlob(Keys.ID_INDEX_KEY_CIPHERTEXT, Keys.ID_INDEX_KEY_NONCE),
        pinVerificationHash = this[Keys.PIN_VERIFICATION_HASH]?.let(::decode),
        pinVerificationSalt = this[Keys.PIN_VERIFICATION_SALT]?.let(::decode),
        recoveryFailedAttemptCount = this[Keys.RECOVERY_FAILED_ATTEMPT_COUNT] ?: 0,
        recoveryLastFailureAt = this[Keys.RECOVERY_LAST_FAILURE_AT_EPOCH_MILLI]?.let(Instant::ofEpochMilli)
    )

    private fun Preferences.readBlob(
        ciphertextKey: Preferences.Key<String>,
        nonceKey: Preferences.Key<String>
    ): EncryptedBlob? {
        val ciphertext = this[ciphertextKey] ?: return null
        val nonce = this[nonceKey] ?: return null
        return EncryptedBlob(decode(ciphertext), decode(nonce))
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeBlob(
        ciphertextKey: Preferences.Key<String>,
        nonceKey: Preferences.Key<String>,
        blob: EncryptedBlob?
    ) {
        if (blob == null) {
            remove(ciphertextKey)
            remove(nonceKey)
        } else {
            this[ciphertextKey] = encode(blob.ciphertext)
            this[nonceKey] = encode(blob.nonce)
        }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeBytes(
        key: Preferences.Key<String>,
        bytes: ByteArray?
    ) {
        if (bytes == null) remove(key) else this[key] = encode(bytes)
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeInt(key: Preferences.Key<Int>, value: Int?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeLong(key: Preferences.Key<Long>, value: Long?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(str: String): ByteArray = Base64.getDecoder().decode(str)
}
