package com.heckmannch.birthdaybuddy.domain.usecase

import android.net.Uri
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to export all gift ideas to the specified destination URI.
 */
@Reusable
class ExportGiftIdeasUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(uri: Uri) {
        contactRepository.exportGiftIdeas(uri)
    }
}
