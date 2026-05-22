package com.heckmannch.birthdaybuddy.data.local

import androidx.room.TypeConverter
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import org.json.JSONArray
import org.json.JSONObject

class GiftIdeaConverters {
    @TypeConverter
    fun fromGiftIdeaList(list: List<GiftIdea>?): String {
        if (list == null) return "[]"
        val jsonArray = JSONArray()
        list.forEach { idea ->
            val obj = JSONObject().apply {
                put("id", idea.id)
                put("text", idea.text)
                put("isChecked", idea.isChecked)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toGiftIdeaList(data: String?): List<GiftIdea> {
        if (data.isNullOrBlank()) return emptyList()

        return try {
            val jsonArray = JSONArray(data)
            List(jsonArray.length()) { i ->
                val obj = jsonArray.getJSONObject(i)
                GiftIdea(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    isChecked = obj.getBoolean("isChecked")
                )
            }
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
