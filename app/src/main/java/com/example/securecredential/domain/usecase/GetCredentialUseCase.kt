package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.repository.CredentialRepository
import javax.inject.Inject

class GetCredentialUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(credentialId: String): Result<Credential> = repository.get(credentialId)
}
