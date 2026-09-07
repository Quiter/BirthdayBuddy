package com.heckmannch.birthdaybuddy.data.local

import androidx.room.TypeConverter
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.util.JsonUtils
import java.time.LocalDate

class Converters {
    companion object {
        private val json = JsonUtils.defaultJson
    }

    @TypeConverter
    fun fromThemeMode(themeMode: ThemeMode?): String? = themeMode?.name

    @TypeConverter
    fun toThemeMode(value: String?): ThemeMode? {
        if (value == null) return null
        return try {
            ThemeMode.valueOf(value)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    @TypeConverter
    fun fromString(value: String?): LocalDate? = value?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list == null) return "[]"
        return json.encodeToString(list)
    }

    @TypeConverter
    fun toList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(data)
        } catch (_: Exception) {
            // Fallback für alte Daten (Pipe-separiert)
            data.split("|").filter { it.isNotBlank() }
        }
    }
}

