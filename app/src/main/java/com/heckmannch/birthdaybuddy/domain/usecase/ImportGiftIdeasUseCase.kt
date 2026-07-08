package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to import gift ideas from a JSON-encoded string.
 * Returns the count of successfully imported gift ideas, or -1 on invalid JSON format.
 */
@Reusable
class ImportGiftIdeasUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(jsonString: String): Int {
        return contactRepository.importGiftIdeas(jsonString)
    }
}
