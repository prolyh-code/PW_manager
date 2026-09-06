package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.repository.CredentialRepository
import javax.inject.Inject

class ListCredentialsUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): Result<List<CredentialSummary>> = repository.listAll()
}
