package com.example.securecredential.core.crypto

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecoveryBackoffPolicyTest {

    @Test
    fun `attempts 1 through 4 require no delay`() {
        for (attempt in 1..4) {
            assertEquals("attempt $attempt", kotlin.time.Duration.ZERO, RecoveryBackoffPolicy.delayForAttempt(attempt))
        }
    }

    @Test
    fun `attempt 0 requires no delay`() {
        assertEquals(kotlin.time.Duration.ZERO, RecoveryBackoffPolicy.delayForAttempt(0))
    }

    @Test
    fun `attempt 5 requires 30 seconds`() {
        assertEquals(30.seconds, RecoveryBackoffPolicy.delayForAttempt(5))
    }

    @Test
    fun `attempt 6 requires 60 seconds`() {
        assertEquals(60.seconds, RecoveryBackoffPolicy.delayForAttempt(6))
    }

    @Test
    fun `attempt 7 requires 120 seconds`() {
        assertEquals(120.seconds, RecoveryBackoffPolicy.delayForAttempt(7))
    }

    @Test
    fun `attempt 8 requires 240 seconds`() {
        assertEquals(240.seconds, RecoveryBackoffPolicy.delayForAttempt(8))
    }

    @Test
    fun `attempt 9 requires 480 seconds`() {
        assertEquals(480.seconds, RecoveryBackoffPolicy.delayForAttempt(9))
    }

    @Test
    fun `attempt 10 and beyond cap at 600 seconds`() {
        assertEquals(600.seconds, RecoveryBackoffPolicy.delayForAttempt(10))
        assertEquals(600.seconds, RecoveryBackoffPolicy.delayForAttempt(11))
        assertEquals(600.seconds, RecoveryBackoffPolicy.delayForAttempt(1000))
    }

    @Test
    fun `negative attempt count is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecoveryBackoffPolicy.delayForAttempt(-1)
        }
    }
}
