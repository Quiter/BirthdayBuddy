package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.CoupleSuggestion
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing married couple relationships, potential couple
 * merge suggestions, and suggestion ignore lists.
 */
interface CoupleRepository {

    /**
     * Reactive stream of potential married couple suggestions inferred from shared anniversaries.
     */
    val potentialCouples: Flow<List<CoupleSuggestion>>

    /**
     * Reactive stream of contact lookup key pairs that have been ignored by the user.
     */
    val ignoredCouples: Flow<List<String>>

    /**
     * Alias for [ignoredCouples] maintaining compatibility with existing consumers.
     */
    val ignoredCouplePairs: Flow<List<String>> get() = ignoredCouples

    /**
     * Links two independent contacts as a married couple across persistent storage and caches.
     *
     * @param lookupKey1 The lookup key of the first contact.
     * @param lookupKey2 The lookup key of the second contact.
     */
    suspend fun linkAsCouple(lookupKey1: String, lookupKey2: String)

    /**
     * Breaks the couple link between a contact and their linked spouse.
     *
     * @param lookupKey The lookup key of either spouse in the linked relationship.
     */
    suspend fun unlinkCouple(lookupKey: String)

    /**
     * Marks a potential couple suggestion as dismissed/ignored so it is no longer recommended.
     *
     * @param lookupKey1 The lookup key of the first contact in the suggested pair.
     * @param lookupKey2 The lookup key of the second contact in the suggested pair.
     */
    suspend fun ignoreSuggestion(lookupKey1: String, lookupKey2: String)

    /**
     * Alias for [ignoreSuggestion] maintaining backwards compatibility.
     *
     * @param lookupKey1 The lookup key of the first contact in the suggested pair.
     * @param lookupKey2 The lookup key of the second contact in the suggested pair.
     */
    suspend fun ignoreCoupleSuggestion(lookupKey1: String, lookupKey2: String) =
        ignoreSuggestion(lookupKey1, lookupKey2)

    /**
     * Clears all previously ignored couple suggestions, resetting them to be shown again.
     */
    suspend fun clearIgnoredCouplePairs()
}
