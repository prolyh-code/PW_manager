package com.example.securecredential.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.securecredential.data.local.entity.UsernameIndexEntity

@Dao
interface UsernameIndexDao {
    @Insert
    suspend fun insert(index: UsernameIndexEntity)

    @Query("SELECT credentialId FROM username_index WHERE tokenHash = :token")
    suspend fun findCredentialIds(token: ByteArray): List<String>

    @Query("DELETE FROM username_index WHERE credentialId = :credentialId")
    suspend fun deleteByCredentialId(credentialId: String)
}
