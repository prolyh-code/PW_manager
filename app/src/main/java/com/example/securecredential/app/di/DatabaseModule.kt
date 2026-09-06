package com.example.securecredential.app.di

import android.content.Context
import androidx.room.Room
import com.example.securecredential.data.local.dao.CredentialDao
import com.example.securecredential.data.local.dao.SearchIndexDao
import com.example.securecredential.data.local.dao.UsernameIndexDao
import com.example.securecredential.data.local.database.SecureVaultDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SecureVaultDatabase =
        Room.databaseBuilder(context, SecureVaultDatabase::class.java, SecureVaultDatabase.DATABASE_NAME).build()

    @Provides
    fun provideCredentialDao(database: SecureVaultDatabase): CredentialDao = database.credentialDao()

    @Provides
    fun provideSearchIndexDao(database: SecureVaultDatabase): SearchIndexDao = database.searchIndexDao()

    @Provides
    fun provideUsernameIndexDao(database: SecureVaultDatabase): UsernameIndexDao = database.usernameIndexDao()
}
