package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.CoupleRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to link two contacts as a married couple.
 */
@Reusable
class LinkAsCoupleUseCase @Inject constructor(
    private val coupleRepository: CoupleRepository
) {
    suspend operator fun invoke(lookupKey1: String, lookupKey2: String) {
        coupleRepository.linkAsCouple(lookupKey1, lookupKey2)
    }
}
