package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable
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
        fun withToggledIdea(
            currentIdeas: List<GiftIdea>,
            idea: GiftIdea,
            isChecked: Boolean
        ): List<GiftIdea> {
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
