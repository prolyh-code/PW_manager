package com.example.securecredential.presentation.settings

import com.example.securecredential.core.crypto.Pbkdf2KeyDeriver
import com.example.securecredential.data.security.CryptoManagerImpl
import com.example.securecredential.data.security.FakeKeyManager
import com.example.securecredential.data.security.InMemoryKeyMaterialStore
import com.example.securecredential.data.security.KeyLifecycleOrchestrator
import com.example.securecredential.data.security.RecoveryKeyManager
import com.example.securecredential.presentation.authentication.FakeAppPreferences
import com.example.securecredential.presentation.authentication.SessionManagerImpl
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful pin change ends the flow and clears the changing-pin state`() = runTest {
        val cryptoManager = CryptoManagerImpl()
        val orchestrator = KeyLifecycleOrchestrator(
            FakeKeyManager(), RecoveryKeyManager(Pbkdf2KeyDeriver()), cryptoManager, InMemoryKeyMaterialStore()
        )
        orchestrator.onFirstLaunch("11112222".toCharArray(), requireBiometric = false)
        val sessionManager = SessionManagerImpl(cryptoManager, FakeAppPreferences(30.seconds), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        val viewModel = SettingsViewModel(FakeAppPreferences(30.seconds), orchestrator, sessionManager)

        viewModel.beginPinChange()
        "11112222".forEach { viewModel.appendPinChangeDigit(it) }
        viewModel.submitOldPin()
        "33334444".forEach { viewModel.appendPinChangeDigit(it) }
        viewModel.submitNewPin()
        "33334444".forEach { viewModel.appendPinChangeDigit(it) }
        viewModel.submitNewPinConfirmation()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isChangingPin)
        assertNotNull(viewModel.uiState.value.infoMessage)
    }

    @Test
    fun `wrong old pin surfaces an error and resets to the old-pin step`() = runTest {
        val cryptoManager = CryptoManagerImpl()
        val orchestrator = KeyLifecycleOrchestrator(
            FakeKeyManager(), RecoveryKeyManager(Pbkdf2KeyDeriver()), cryptoManager, InMemoryKeyMaterialStore()
        )
        orchestrator.onFirstLaunch("11112222".toCharArray(), requireBiometric = false)
        val sessionManager = SessionManagerImpl(cryptoManager, FakeAppPreferences(30.seconds), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        val viewModel = SettingsViewModel(FakeAppPreferences(30.seconds), orchestrator, sessionManager)

        viewModel.beginPinChange()
        "99998888".forEach { viewModel.appendPinChangeDigit(it) } // wrong old PIN
        viewModel.submitOldPin()
        "33334444".forEach { viewModel.appendPinChangeDigit(it) }
        viewModel.submitNewPin()
        "33334444".forEach { viewModel.appendPinChangeDigit(it) }
        viewModel.submitNewPinConfirmation()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(PinChangeStep.OLD_PIN, viewModel.uiState.value.pinChangeStep)
        assertEquals(true, viewModel.uiState.value.isChangingPin) // stays open so the user can retry
    }

    @Test
    fun `mismatched new pin confirmation is rejected before touching the orchestrator`() = runTest {
        val cryptoManager = CryptoManagerImpl()
        val orchestrator = KeyLifecycleOrchestrator(
            FakeKeyManager(), RecoveryKeyManager(Pbkdf2KeyDeriver()), cryptoManager, InMemoryKeyMaterialStore()
        )
        orchestrator.onFirstLaunch("11112222".toCharArray(), requireBiometric = false)
        val sessionManager = SessionManagerImpl(cryptoManager, FakeAppPreferences(30.seconds), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        val viewModel = SettingsViewModel(FakeAppPreferences(30.seconds), orchestrator, sessionManager)

        viewModel.beginPinChange()
        "11112222".forEach { viewModel.appendPinChangeDigit(it) }
        viewModel.submitOldPin()
        "33334444".forEach { viewModel.appendPinChangeDigit(it) }
        viewModel.submitNewPin()
        "00000000".forEach { viewModel.appendPinChangeDigit(it) } // does not match
        viewModel.submitNewPinConfirmation()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(PinChangeStep.NEW_PIN, viewModel.uiState.value.pinChangeStep)
        assertEquals("", viewModel.uiState.value.newPin)
    }
}
