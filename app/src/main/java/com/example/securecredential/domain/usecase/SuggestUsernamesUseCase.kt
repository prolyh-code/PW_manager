package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.repository.CredentialRepository
import javax.inject.Inject

/** Spec 8.4 — Exact Match ID autocomplete. */
class SuggestUsernamesUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(username: String): Result<List<CredentialSummary>> = repository.findByUsername(username)
}
