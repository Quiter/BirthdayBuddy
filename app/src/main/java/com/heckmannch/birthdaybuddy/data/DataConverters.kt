package com.heckmannch.birthdaybuddy.data

import androidx.room.TypeConverter
import com.heckmannch.birthdaybuddy.model.ContactActions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun fromContactActions(value: ContactActions): String = Json.encodeToString(value)

    @TypeConverter
    fun toContactActions(value: String): ContactActions = Json.decodeFromString(value)
}
