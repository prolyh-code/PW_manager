package com.example.securecredential.presentation.authentication

import com.example.securecredential.core.crypto.SymmetricKeyGenerator
import com.example.securecredential.data.security.CryptoManagerImpl
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A mutable java.time.Clock test double — java.time's own fixed() clock can't advance. */
private class MutableClock(
    private var current: Instant,
    private val zone: ZoneId = ZoneOffset.UTC
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
    override fun instant(): Instant = current
    fun advanceBySeconds(seconds: Long) {
        current = current.plusSeconds(seconds)
    }
}

class SessionManagerImplTest {

    private fun newSessionManager(
        clock: MutableClock,
        timeoutSeconds: Long = 30
    ): Pair<SessionManagerImpl, CryptoManagerImpl> {
        val cryptoManager = CryptoManagerImpl()
        val sessionManager = SessionManagerImpl(
            cryptoManager, FakeAppPreferences(timeoutSeconds.seconds), clock
        )
        return sessionManager to cryptoManager
    }

    @Test
    fun `initial state is LOCKED and MASKED`() {
        val (sessionManager, _) = newSessionManager(MutableClock(Instant.EPOCH))

        assertEquals(AppSessionState.LOCKED, sessionManager.appState.value)
        assertEquals(CredentialDisplayState.MASKED, sessionManager.credentialDisplayState.value)
    }

    @Test
    fun `unlock and displayCredential transition state as expected`() {
        val (sessionManager, _) = newSessionManager(MutableClock(Instant.EPOCH))

        sessionManager.unlock()
        sessionManager.displayCredential()

        assertEquals(AppSessionState.UNLOCKED, sessionManager.appState.value)
        assertEquals(CredentialDisplayState.DISPLAYED, sessionManager.credentialDisplayState.value)
    }

    @Test
    fun `backgrounding masks immediately even though appState stays UNLOCKED (spec 9_1 independence)`() {
        val (sessionManager, _) = newSessionManager(MutableClock(Instant.EPOCH))
        sessionManager.unlock()
        sessionManager.displayCredential()

        sessionManager.onAppBackgrounded()

        assertEquals(CredentialDisplayState.MASKED, sessionManager.credentialDisplayState.value)
        assertEquals("appState must not change just from backgrounding", AppSessionState.UNLOCKED, sessionManager.appState.value)
    }

    @Test
    fun `foregrounding before the auto-lock timeout elapses leaves appState UNLOCKED`() = runTest {
        val clock = MutableClock(Instant.EPOCH)
        val (sessionManager, _) = newSessionManager(clock, timeoutSeconds = 30)
        sessionManager.unlock()

        sessionManager.onAppBackgrounded()
        clock.advanceBySeconds(10) // well under the 30s timeout
        sessionManager.onAppForegrounded()

        assertEquals(AppSessionState.UNLOCKED, sessionManager.appState.value)
    }

    @Test
    fun `foregrounding after the auto-lock timeout elapses locks and clears crypto keys`() = runTest {
        val clock = MutableClock(Instant.EPOCH)
        val (sessionManager, cryptoManager) = newSessionManager(clock, timeoutSeconds = 30)
        cryptoManager.loadKeys(
            SymmetricKeyGenerator.generateAesKey(), SymmetricKeyGenerator.generateHmacKey(), SymmetricKeyGenerator.generateHmacKey()
        )
        sessionManager.unlock()
        assertTrue(cryptoManager.isUnlocked)

        sessionManager.onAppBackgrounded()
        clock.advanceBySeconds(31) // just over the 30s timeout
        sessionManager.onAppForegrounded()

        assertEquals(AppSessionState.LOCKED, sessionManager.appState.value)
        assertFalse("lock() must clear CryptoManager's in-memory keys", cryptoManager.isUnlocked)
    }

    @Test
    fun `foregrounding while already LOCKED does not crash and stays LOCKED`() = runTest {
        val clock = MutableClock(Instant.EPOCH)
        val (sessionManager, _) = newSessionManager(clock)

        sessionManager.onAppBackgrounded()
        clock.advanceBySeconds(100)
        sessionManager.onAppForegrounded()

        assertEquals(AppSessionState.LOCKED, sessionManager.appState.value)
    }

    @Test
    fun `foregrounding without a prior backgrounding is a no-op`() = runTest {
        val (sessionManager, _) = newSessionManager(MutableClock(Instant.EPOCH))
        sessionManager.unlock()

        sessionManager.onAppForegrounded()

        assertEquals(AppSessionState.UNLOCKED, sessionManager.appState.value)
    }

    @Test
    fun `resetCredentialDisplay masks without touching appState (Security Reset, spec 11)`() {
        val (sessionManager, _) = newSessionManager(MutableClock(Instant.EPOCH))
        sessionManager.unlock()
        sessionManager.displayCredential()

        sessionManager.resetCredentialDisplay()

        assertEquals(CredentialDisplayState.MASKED, sessionManager.credentialDisplayState.value)
        assertEquals(AppSessionState.UNLOCKED, sessionManager.appState.value)
    }
}
