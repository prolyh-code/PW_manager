package com.example.securecredential.app.di

import com.example.securecredential.data.repository.CredentialRepositoryImpl
import com.example.securecredential.data.repository.DecryptAllPasswordReuseChecker
import com.example.securecredential.data.repository.PasswordReuseChecker
import com.example.securecredential.domain.repository.CredentialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCredentialRepository(impl: CredentialRepositoryImpl): CredentialRepository

    @Binds
    @Singleton
    abstract fun bindPasswordReuseChecker(impl: DecryptAllPasswordReuseChecker): PasswordReuseChecker
}
