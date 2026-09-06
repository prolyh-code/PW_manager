package com.example.securecredential.domain.usecase

import com.example.securecredential.core.normalization.DomainNormalizer
import javax.inject.Inject

/** Spec 8.1 — live preview while a user types a URL/domain in the Registration form, including
 *  the "accounts.google.com" -> "Google" Service Name suggestion the user can then edit. */
class NormalizeDomainUseCase @Inject constructor() {
    data class Result(val normalizedDomain: String, val suggestedServiceName: String)

    operator fun invoke(rawInput: String): Result {
        val normalized = DomainNormalizer.normalizeDomain(rawInput)
        return Result(normalized, DomainNormalizer.suggestServiceName(normalized))
    }
}
