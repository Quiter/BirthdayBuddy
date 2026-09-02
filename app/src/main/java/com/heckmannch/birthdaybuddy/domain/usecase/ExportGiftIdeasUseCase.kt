package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to export all gift ideas to the specified destination URI string.
 */
@Reusable
class ExportGiftIdeasUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(uriString: String) {
        contactRepository.exportGiftIdeas(uriString)
    }
}
