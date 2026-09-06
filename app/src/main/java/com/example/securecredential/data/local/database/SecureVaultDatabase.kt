package com.example.securecredential.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.securecredential.data.local.dao.CredentialDao
import com.example.securecredential.data.local.dao.SearchIndexDao
import com.example.securecredential.data.local.dao.UsernameIndexDao
import com.example.securecredential.data.local.entity.CredentialEntity
import com.example.securecredential.data.local.entity.SearchIndexEntity
import com.example.securecredential.data.local.entity.UsernameIndexEntity

/**
 * version = 1, no Migration objects yet — there is no prior shipped schema to migrate from.
 * The moment this ships to a real user, every future schema change MUST supply a real
 * Migration; never use fallbackToDestructiveMigration() on a credential store (that's silent
 * password-vault data loss).
 */
@Database(
    entities = [CredentialEntity::class, SearchIndexEntity::class, UsernameIndexEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SecureVaultDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
    abstract fun searchIndexDao(): SearchIndexDao
    abstract fun usernameIndexDao(): UsernameIndexDao

    companion object {
        const val DATABASE_NAME = "securevault.db"
    }
}
