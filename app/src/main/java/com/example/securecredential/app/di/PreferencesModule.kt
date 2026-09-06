package com.example.securecredential.app.di

import com.example.securecredential.data.preferences.AppPreferences
import com.example.securecredential.data.preferences.AppPreferencesImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    @Singleton
    abstract fun bindAppPreferences(impl: AppPreferencesImpl): AppPreferences
}
