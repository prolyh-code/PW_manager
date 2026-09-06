package com.example.securecredential.data.local.database

import androidx.room.TypeConverter
import com.example.securecredential.data.local.entity.SearchType

class Converters {
    @TypeConverter
    fun fromSearchType(value: SearchType): String = value.name

    @TypeConverter
    fun toSearchType(value: String): SearchType = SearchType.valueOf(value)
}
