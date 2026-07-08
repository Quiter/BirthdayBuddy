package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to ignore a potential couple suggestion so it is not shown again.
 */
@Reusable
class IgnoreCoupleSuggestionUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(lookupKey1: String, lookupKey2: String) {
        contactRepository.ignoreCoupleSuggestion(lookupKey1, lookupKey2)
    }
}
