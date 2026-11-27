package com.heckmannch.birthdaybuddy.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.util.JsonUtils
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room type converters for custom data types used in database entities,
 * including [ThemeMode], [LocalDate], lists of strings, and JSON map helpers.
 */
@Singleton
class Converters @Inject constructor() {
    companion object {
        private const val TAG = "Converters"
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
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim Parsen des Datums: $it", e)
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
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim Deserialisieren der String-Liste als JSON, nutze Pipe-Fallback", e)
            // Fallback for legacy data (pipe-separated)
            data.split("|").filter { it.isNotBlank() }
        }
    }

    /**
     * Serializes a map of custom event keys to ISO-8601 [LocalDate] strings into a JSON object.
     */
    fun fromCustomEventsMap(map: Map<String, LocalDate>?): String {
        if (map.isNullOrEmpty()) return "{}"
        val stringMap = map.mapValues { (_, date) -> date.toString() }
        return json.encodeToString(stringMap)
    }

    /**
     * Deserializes a JSON object string into a map of custom event keys to [LocalDate].
     * Invalid date entries are skipped defensively.
     */
    fun toCustomEventsMap(data: String?): Map<String, LocalDate> {
        if (data.isNullOrBlank() || data == "{}") return emptyMap()
        return try {
            val rawMap = json.decodeFromString<Map<String, String>>(data)
            buildMap {
                rawMap.forEach { (key, dateStr) ->
                    val parsed = fromString(dateStr)
                    if (key.isNotBlank() && parsed != null) {
                        put(key, parsed)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim Deserialisieren der Custom-Events-Map", e)
            emptyMap()
        }
    }

    /**
     * Serializes a map of event keys to ARGB color integers into a JSON object.
     */
    fun fromIntMap(map: Map<String, Int>?): String {
        if (map.isNullOrEmpty()) return "{}"
        return json.encodeToString(map)
    }

    /**
     * Deserializes a JSON object string into a map of event keys to ARGB color integers.
     */
    fun toIntMap(data: String?): Map<String, Int> {
        if (data.isNullOrBlank() || data == "{}") return emptyMap()
        return try {
            json.decodeFromString<Map<String, Int>>(data)
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim Deserialisieren der Int-Map", e)
            emptyMap()
        }
    }
}
