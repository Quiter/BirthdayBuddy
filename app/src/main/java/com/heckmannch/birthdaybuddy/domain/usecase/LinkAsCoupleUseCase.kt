package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to link two contacts as a married couple.
 */
@Reusable
class LinkAsCoupleUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(lookupKey1: String, lookupKey2: String) {
        contactRepository.linkAsCouple(lookupKey1, lookupKey2)
    }
}
