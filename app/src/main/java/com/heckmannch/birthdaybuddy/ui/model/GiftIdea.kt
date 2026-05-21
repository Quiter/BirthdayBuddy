package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Modell für eine Geschenkidee.
 */
@Immutable
data class GiftIdea(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
) {
    companion object {
        /**
         * Dekodiert den in der DB gespeicherten String in eine Liste von Objekten.
         */
        fun fromString(encoded: String?): List<GiftIdea> {
            if (encoded.isNullOrBlank()) return emptyList()
            
            return try {
                val jsonArray = JSONArray(encoded)
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
                encoded.split(";;").mapNotNull {
                    val parts = it.split("|", limit = 3)
                    when (parts.size) {
                        3 -> GiftIdea(id = parts[0], isChecked = parts[1] == "1", text = parts[2])
                        2 -> GiftIdea(isChecked = parts[0] == "1", text = parts[1])
                        else -> null
                    }
                }
            }
        }

        /**
         * Enkodiert die Liste für die Speicherung in der Datenbank.
         */
        fun toString(ideas: List<GiftIdea>): String {
            val jsonArray = JSONArray()
            ideas.forEach { idea ->
                val obj = JSONObject().apply {
                    put("id", idea.id)
                    put("text", idea.text)
                    put("isChecked", idea.isChecked)
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        /**
         * Fügt eine neue Idee hinzu und sortiert sie vor die bereits erledigten.
         */
        fun withNewIdea(currentIdeas: List<GiftIdea>, newIdea: GiftIdea): List<GiftIdea> {
            val ideas = currentIdeas.toMutableList()
            val firstCheckedIndex = ideas.indexOfFirst { it.isChecked }
            if (firstCheckedIndex != -1) ideas.add(firstCheckedIndex, newIdea)
            else ideas.add(newIdea)
            return ideas
        }

        /**
         * Ändert den Status einer Idee und sortiert sie entsprechend um.
         */
        fun withToggledIdea(currentIdeas: List<GiftIdea>, idea: GiftIdea, isChecked: Boolean): List<GiftIdea> {
            val ideas = currentIdeas.toMutableList()
            val idx = ideas.indexOfFirst { it.id == idea.id }
            if (idx == -1) return currentIdeas

            ideas.removeAt(idx)
            val newItem = idea.copy(isChecked = isChecked)
            if (isChecked) {
                ideas.add(newItem) // Erledigt kommt ans Ende
            } else {
                val firstCheckedIndex = ideas.indexOfFirst { it.isChecked }
                if (firstCheckedIndex != -1) ideas.add(firstCheckedIndex, newItem)
                else ideas.add(0, newItem)
            }
            return ideas
        }
    }
}
