package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.GiftIdea

/**
 * Domain repository interface for managing contact gift ideas, including CRUD operations
 * and backup export/import functionality.
 */
interface GiftIdeaRepository {

    /**
     * Exports all persisted gift ideas across contacts to the target URI in JSON format.
     *
     * @param uriString The URI string representing the destination document.
     */
    suspend fun exportGiftIdeas(uriString: String)

    /**
     * Imports gift ideas from the specified source document URI, updating contact records
     * and merging with existing ideas.
     *
     * @param uriString The URI string pointing to the source JSON document.
     * @return The number of successfully imported gift ideas, or -1 if the file could not be read or parsed.
     */
    suspend fun importGiftIdeas(uriString: String): Int

    /**
     * Adds a new gift idea to the contact identified by [lookupKey].
     *
     * @param lookupKey The stable system lookup key of the target contact.
     * @param newIdea The new [GiftIdea] domain object to append.
     */
    suspend fun addGiftIdea(lookupKey: String, newIdea: GiftIdea)

    /**
     * Removes an existing gift idea from the specified contact or their linked spouse.
     *
     * @param lookupKey The lookup key of the contact or spouse associated with the idea.
     * @param ideaId The unique identifier of the gift idea to remove.
     */
    suspend fun removeGiftIdea(lookupKey: String, ideaId: String)

    /**
     * Deletes an existing gift idea item. Alias for [removeGiftIdea] for API compatibility.
     *
     * @param lookupKey The lookup key of the contact associated with the idea.
     * @param ideaId The unique identifier of the gift idea to remove.
     */
    suspend fun deleteGiftIdea(lookupKey: String, ideaId: String) = removeGiftIdea(lookupKey, ideaId)

    /**
     * Toggles the checked/purchased state of an existing gift idea.
     *
     * @param lookupKey The lookup key of the target contact or their linked spouse.
     * @param idea The [GiftIdea] to toggle.
     * @param isChecked Whether the gift idea is marked as purchased/done.
     */
    suspend fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean)

    /**
     * Updates the description text of an existing gift idea.
     *
     * @param lookupKey The lookup key of the contact or spouse owning the idea.
     * @param ideaId The unique identifier of the gift idea to update.
     * @param newText The updated description text.
     */
    suspend fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String)
}
