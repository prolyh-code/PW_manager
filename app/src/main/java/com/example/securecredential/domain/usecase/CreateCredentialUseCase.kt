package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.repository.CredentialRepository
import javax.inject.Inject

class CreateCredentialUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(credential: Credential): Result<String> = repository.create(credential)
}
