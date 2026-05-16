package com.heckmannch.birthdaybuddy.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromString(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list == null) return "[]"
        return JSONArray(list).toString()
    }

    @TypeConverter
    fun toList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(data)
            List(jsonArray.length()) { jsonArray.getString(it) }
        } catch (_: Exception) {
            // Fallback für alte Daten (Pipe-separiert)
            data.split("|").filter { it.isNotBlank() }
        }
    }
}
