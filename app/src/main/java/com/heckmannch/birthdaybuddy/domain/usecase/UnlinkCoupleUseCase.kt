package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to break the couple link between spouses.
 */
@Reusable
class UnlinkCoupleUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(lookupKey: String) {
        contactRepository.unlinkCouple(lookupKey)
    }
}
