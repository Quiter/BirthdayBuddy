package com.heckmannch.birthdaybuddy.data.local

import androidx.room.TypeConverter
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.util.JsonUtils

/**
 * Room type converters for serializing and deserializing lists of [GiftIdea] objects.
 */
class GiftIdeaConverters {
    companion object {
        private val json = JsonUtils.defaultJson
    }

    /**
     * Serializes a list of [GiftIdea] objects into a JSON string.
     */
    @TypeConverter
    fun fromGiftIdeaList(list: List<GiftIdea>?): String {
        if (list == null) return "[]"
        return json.encodeToString(list)
    }

    /**
     * Deserializes a JSON string into a list of [GiftIdea] objects, falling back to legacy format if needed.
     */
    @TypeConverter
    fun toGiftIdeaList(data: String?): List<GiftIdea> {
        if (data.isNullOrBlank()) return emptyList()

        return try {
            json.decodeFromString<List<GiftIdea>>(data)
        } catch (_: Exception) {
            // Fallback for legacy format (separated by ';;' and '|')
            data.split(";;").mapNotNull {
                val parts = it.split("|", limit = 3)
                when (parts.size) {
                    3 -> GiftIdea(id = parts[0], isChecked = parts[1] == "1", text = parts[2])
                    2 -> GiftIdea(isChecked = parts[0] == "1", text = parts[1])
                    else -> null
                }
            }
        }
    }
}
