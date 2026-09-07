package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.CoupleRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to ignore a potential couple suggestion so it is not shown again.
 */
@Reusable
class IgnoreCoupleSuggestionUseCase @Inject constructor(
    private val coupleRepository: CoupleRepository
) {
    suspend operator fun invoke(lookupKey1: String, lookupKey2: String) {
        coupleRepository.ignoreCoupleSuggestion(lookupKey1, lookupKey2)
    }
}
