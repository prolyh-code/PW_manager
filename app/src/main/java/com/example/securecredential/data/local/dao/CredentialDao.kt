package com.example.securecredential.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.securecredential.data.local.entity.CredentialEntity

@Dao
interface CredentialDao {
    @Insert
    suspend fun insert(credential: CredentialEntity)

    @Update
    suspend fun update(credential: CredentialEntity)

    @Delete
    suspend fun delete(credential: CredentialEntity)

    @Query("SELECT * FROM credentials WHERE credentialId = :id")
    suspend fun findById(id: String): CredentialEntity?

    /** Also used for Password Reuse Detection's decrypt-and-compare pass (spec 7). */
    @Query("SELECT * FROM credentials")
    suspend fun getAll(): List<CredentialEntity>
}
