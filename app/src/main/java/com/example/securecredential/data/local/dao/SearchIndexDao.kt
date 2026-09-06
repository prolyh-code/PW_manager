package com.example.securecredential.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.securecredential.data.local.entity.SearchIndexEntity

@Dao
interface SearchIndexDao {
    @Insert
    suspend fun insert(index: SearchIndexEntity)

    @Query("SELECT credentialId FROM search_index WHERE tokenHash = :token")
    suspend fun findCredentialIds(token: ByteArray): List<String>

    @Query("DELETE FROM search_index WHERE credentialId = :credentialId")
    suspend fun deleteByCredentialId(credentialId: String)
}
