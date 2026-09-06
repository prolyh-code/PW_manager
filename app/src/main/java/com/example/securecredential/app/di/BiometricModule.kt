package com.example.securecredential.app.di

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.example.securecredential.data.security.BiometricAuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

/** Activity-scoped: BiometricPrompt requires a FragmentActivity/Fragment host (MainActivity extends FragmentActivity). */
@Module
@InstallIn(ActivityComponent::class)
object BiometricModule {
    @Provides
    @ActivityScoped
    fun provideBiometricAuthManager(activity: Activity): BiometricAuthManager =
        BiometricAuthManager(activity as FragmentActivity)
}
