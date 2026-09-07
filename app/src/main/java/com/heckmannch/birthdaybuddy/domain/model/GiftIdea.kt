package com.heckmannch.birthdaybuddy.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Domain model representing a gift idea for a contact.
 */
@Serializable
data class GiftIdea(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
) {
    companion object {
        /**
         * Adds a new idea and sorts it before any already completed items.
         */
        fun withNewIdea(currentIdeas: List<GiftIdea>, newIdea: GiftIdea): List<GiftIdea> {
            val ideas = currentIdeas.toMutableList()
            val firstCheckedIndex = ideas.indexOfFirst { it.isChecked }
            if (firstCheckedIndex != -1) ideas.add(firstCheckedIndex, newIdea)
            else ideas.add(newIdea)
            return ideas
        }

        /**
         * Toggles the checked status of an idea and updates its sorting accordingly.
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
                ideas.add(newItem) // Completed items move to the end
            } else {
                val firstCheckedIndex = ideas.indexOfFirst { it.isChecked }
                if (firstCheckedIndex != -1) ideas.add(firstCheckedIndex, newItem)
                else ideas.add(0, newItem)
            }
            return ideas
        }
    }
}
