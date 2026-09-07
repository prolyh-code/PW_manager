package com.example.securecredential.app

import android.app.Application
import com.example.securecredential.core.lifecycle.AppLifecycleObserver
import com.example.securecredential.core.util.AppLanguage
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SecureCredentialApp : Application() {

    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        AppLanguage.applyDefaultIfUnset()
        appLifecycleObserver.start()
    }
}
