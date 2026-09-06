package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.repository.CredentialRepository
import javax.inject.Inject

class DeleteCredentialUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(credentialId: String): Result<Unit> = repository.delete(credentialId)
}
