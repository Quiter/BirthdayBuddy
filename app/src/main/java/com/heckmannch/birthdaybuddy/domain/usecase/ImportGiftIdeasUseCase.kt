package com.heckmannch.birthdaybuddy.domain.usecase

import android.net.Uri
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to import gift ideas from the specified source URI.
 * Returns the count of successfully imported gift ideas, or -1 on invalid format.
 */
@Reusable
class ImportGiftIdeasUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(uri: Uri): Int {
        return contactRepository.importGiftIdeas(uri)
    }
}
