package com.example.securecredential.core.util

/**
 * Spec 5.2: minimum 8+ digit numeric, OR 6+ character alphanumeric mix (stronger than a
 * typical 4-digit app PIN because this PIN also derives the Recovery KEK).
 */
object PinPolicy {
    const val MIN_NUMERIC_LENGTH = 8
    const val MIN_ALPHANUMERIC_LENGTH = 6

    fun isValid(pin: String): Boolean {
        val isAllDigits = pin.isNotEmpty() && pin.all { it.isDigit() }
        if (isAllDigits) return pin.length >= MIN_NUMERIC_LENGTH

        val hasDigit = pin.any { it.isDigit() }
        val hasLetter = pin.any { it.isLetter() }
        return pin.length >= MIN_ALPHANUMERIC_LENGTH && hasDigit && hasLetter
    }
}
