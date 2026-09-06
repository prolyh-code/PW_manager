package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.model.SearchQuery
import com.example.securecredential.domain.repository.CredentialRepository
import javax.inject.Inject

/** Spec 8.2 Exact Search only for MVP — an empty result means "show the no-exact-match hint" (8.3). */
class SearchCredentialsUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(query: SearchQuery): Result<List<CredentialSummary>> = repository.search(query)
}
