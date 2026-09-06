package com.example.securecredential.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.securecredential.presentation.authentication.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Bridges Android's app-wide (ProcessLifecycleOwner) foreground/background transitions into
 * [SessionManager] (spec 9.1). [start] is called once from SecureCredentialApp.onCreate().
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val sessionManager: SessionManager,
    private val applicationScope: CoroutineScope
) : DefaultLifecycleObserver {

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        sessionManager.onAppBackgrounded()
    }

    override fun onStart(owner: LifecycleOwner) {
        applicationScope.launch {
            sessionManager.onAppForegrounded()
        }
    }
}
