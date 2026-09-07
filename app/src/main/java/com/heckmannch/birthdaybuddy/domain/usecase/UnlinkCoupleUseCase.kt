package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.CoupleRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to break the couple link between spouses.
 */
@Reusable
class UnlinkCoupleUseCase @Inject constructor(
    private val coupleRepository: CoupleRepository
) {
    suspend operator fun invoke(lookupKey: String) {
        coupleRepository.unlinkCouple(lookupKey)
    }
}
