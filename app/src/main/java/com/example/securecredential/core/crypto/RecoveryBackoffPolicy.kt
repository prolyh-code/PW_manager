package com.example.securecredential.core.crypto

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Recovery-unwrap lockout schedule (spec 5.2 / Open Item #2 resolution). Pure function of the
 * consecutive-failure count; callers own tracking the count and the last-failure timestamp.
 */
object RecoveryBackoffPolicy {
    fun delayForAttempt(consecutiveFailedAttempts: Int): Duration {
        require(consecutiveFailedAttempts >= 0) { "consecutiveFailedAttempts must be >= 0" }
        return when {
            consecutiveFailedAttempts <= 4 -> Duration.ZERO
            consecutiveFailedAttempts == 5 -> 30.seconds
            consecutiveFailedAttempts == 6 -> 60.seconds
            consecutiveFailedAttempts == 7 -> 120.seconds
            consecutiveFailedAttempts == 8 -> 240.seconds
            consecutiveFailedAttempts == 9 -> 480.seconds
            else -> 600.seconds
        }
    }
}
