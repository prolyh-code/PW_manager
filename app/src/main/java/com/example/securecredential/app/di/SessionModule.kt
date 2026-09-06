package com.example.securecredential.app.di

import com.example.securecredential.presentation.authentication.SessionManager
import com.example.securecredential.presentation.authentication.SessionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds
    @Singleton
    abstract fun bindSessionManager(impl: SessionManagerImpl): SessionManager

    companion object {
        // Dagger does not honor Kotlin default parameter values, so a Clock binding must be
        // provided explicitly for SessionManagerImpl's @Inject constructor to resolve.
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
