package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.repository.CredentialRepository
import javax.inject.Inject

class UpdateCredentialUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(credential: Credential): Result<Unit> = repository.update(credential)
}
