package com.example.securecredential.app.di

import com.example.securecredential.core.crypto.Pbkdf2KeyDeriver
import com.example.securecredential.core.crypto.PinKeyDeriver
import com.example.securecredential.data.preferences.DataStoreKeyMaterialStore
import com.example.securecredential.data.security.AndroidKeystoreKeyManager
import com.example.securecredential.data.security.CryptoManager
import com.example.securecredential.data.security.CryptoManagerImpl
import com.example.securecredential.data.security.KeyLifecycleOrchestrator
import com.example.securecredential.data.security.KeyManager
import com.example.securecredential.data.security.KeyMaterialStore
import com.example.securecredential.data.security.RecoveryKeyManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-wide (not Activity-scoped) crypto/key-management bindings. BiometricAuthManager is
 * separate (see BiometricModule) since it needs a FragmentActivity.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindCryptoManager(impl: CryptoManagerImpl): CryptoManager

    @Binds
    @Singleton
    abstract fun bindKeyMaterialStore(impl: DataStoreKeyMaterialStore): KeyMaterialStore

    @Binds
    abstract fun bindPinKeyDeriver(impl: Pbkdf2KeyDeriver): PinKeyDeriver

    companion object {
        @Provides
        @Singleton
        fun provideKeyManager(): KeyManager = AndroidKeystoreKeyManager()

        @Provides
        @Singleton
        fun provideRecoveryKeyManager(pinKeyDeriver: PinKeyDeriver): RecoveryKeyManager =
            RecoveryKeyManager(pinKeyDeriver)

        @Provides
        @Singleton
        fun provideKeyLifecycleOrchestrator(
            keyManager: KeyManager,
            recoveryKeyManager: RecoveryKeyManager,
            cryptoManager: CryptoManager,
            keyMaterialStore: KeyMaterialStore
        ): KeyLifecycleOrchestrator =
            KeyLifecycleOrchestrator(keyManager, recoveryKeyManager, cryptoManager, keyMaterialStore)
    }
}
