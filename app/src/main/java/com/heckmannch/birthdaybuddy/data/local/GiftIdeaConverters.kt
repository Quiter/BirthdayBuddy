package com.heckmannch.birthdaybuddy.data.local

import androidx.room.TypeConverter
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import kotlinx.serialization.json.Json

class GiftIdeaConverters {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    @TypeConverter
    fun fromGiftIdeaList(list: List<GiftIdea>?): String {
        if (list == null) return "[]"
        return json.encodeToString(list)
    }

    @TypeConverter
    fun toGiftIdeaList(data: String?): List<GiftIdea> {
        if (data.isNullOrBlank()) return emptyList()

        return try {
            json.decodeFromString<List<GiftIdea>>(data)
        } catch (_: Exception) {
            // Fallback für das alte Format (;; und | separiert)
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

