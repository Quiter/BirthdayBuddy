package com.heckmannch.birthdaybuddy.data.local

import androidx.room.TypeConverter
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.util.JsonUtils
import java.time.LocalDate

/**
 * Room type converters for custom data types used in database entities,
 * including [ThemeMode], [LocalDate], and lists of strings.
 */
class Converters {
    companion object {
        private val json = JsonUtils.defaultJson
    }

    /**
     * Converts a [ThemeMode] to its string representation.
     */
    @TypeConverter
    fun fromThemeMode(themeMode: ThemeMode?): String? = themeMode?.name

    /**
     * Converts a string to a [ThemeMode], falling back to [ThemeMode.SYSTEM] if parsing fails.
     */
    @TypeConverter
    fun toThemeMode(value: String?): ThemeMode? {
        if (value == null) return null
        return try {
            ThemeMode.valueOf(value)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    /**
     * Converts an ISO-8601 date string to a [LocalDate].
     */
    @TypeConverter
    fun fromString(value: String?): LocalDate? = value?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts a [LocalDate] to an ISO-8601 date string.
     */
    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    /**
     * Serializes a list of strings into a JSON array string.
     */
    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list == null) return "[]"
        return json.encodeToString(list)
    }

    /**
     * Deserializes a JSON array string into a list of strings, falling back to pipe-separated values for legacy data.
     */
    @TypeConverter
    fun toList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(data)
        } catch (_: Exception) {
            // Fallback for legacy data (pipe-separated)
            data.split("|").filter { it.isNotBlank() }
        }
    }
}
