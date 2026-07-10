package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.CoupleSuggestion
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.util.getInitials
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Evaluates potential couples and ignored couples configurations, producing a single,
 * active couple suggestion for display on the home screen when the anniversary filter is selected.
 *
 * Design decision: Returns the clean domain model [CoupleSuggestion] instead of the UI model
 * [com.heckmannch.birthdaybuddy.ui.model.CoupleSuggestionUiModel] to preserve clean architecture
 * layering and avoid UI dependencies in the domain layer.
 */
@Reusable
class GetCoupleSuggestionUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(selectedLabel: Flow<String?>): Flow<CoupleSuggestion?> = combine(
        contactRepository.potentialCouples,
        contactRepository.ignoredCouplePairs,
        selectedLabel
    ) { potentials, ignoredPairs, label ->
        if (label != ContactLabels.LABEL_ANNIVERSARY || potentials.isEmpty()) return@combine null

        potentials.firstOrNull { couple ->
            val pairKey = if (couple.firstLookupKey < couple.secondLookupKey) {
                "${couple.firstLookupKey}:${couple.secondLookupKey}"
            } else {
                "${couple.secondLookupKey}:${couple.firstLookupKey}"
            }
            !ignoredPairs.contains(pairKey)
        }?.let { couple ->
            CoupleSuggestion(
                firstLookupKey = couple.firstLookupKey,
                firstName = couple.firstName,
                firstImageUri = couple.firstImageUri,
                firstInitials = couple.firstName.getInitials(),
                secondLookupKey = couple.secondLookupKey,
                secondName = couple.secondName,
                secondImageUri = couple.secondImageUri,
                secondInitials = couple.secondName.getInitials()
            )
        }
    }
}
