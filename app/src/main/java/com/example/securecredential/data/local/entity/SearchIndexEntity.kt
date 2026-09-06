package com.example.securecredential.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SearchType { SERVICE, DOMAIN }

/** Spec section 6.1. tokenHash = HMAC(SearchKey, "domain:"+value or "service:"+value). */
@Entity(tableName = "search_index", indices = [Index(value = ["tokenHash"])])
data class SearchIndexEntity(
    @PrimaryKey val indexId: String,
    val tokenHash: ByteArray,
    val credentialId: String,
    val searchType: SearchType
)
