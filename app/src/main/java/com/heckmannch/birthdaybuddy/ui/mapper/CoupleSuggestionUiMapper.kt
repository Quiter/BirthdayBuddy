package com.heckmannch.birthdaybuddy.ui.mapper

import com.heckmannch.birthdaybuddy.domain.model.CoupleSuggestion
import com.heckmannch.birthdaybuddy.ui.model.CoupleSuggestionUiModel
import com.heckmannch.birthdaybuddy.util.getInitials
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper for converting domain [CoupleSuggestion] models to [CoupleSuggestionUiModel]s.
 * Decouples domain logic from presentation/Compose-specific immutability annotations
 * and handles UI-specific presentation formatting like avatar initials to support Clean Architecture guidelines.
 */
@Reusable
class CoupleSuggestionUiMapper @Inject constructor() {
    /**
     * Maps a domain [CoupleSuggestion] model to [CoupleSuggestionUiModel].
     *
     * @param domain The domain model representing a couple suggestion.
     * @return The mapped UI model suitable for representation in Composable screens.
     */
    fun toUiModel(domain: CoupleSuggestion): CoupleSuggestionUiModel {
        return CoupleSuggestionUiModel(
            firstLookupKey = domain.firstLookupKey,
            firstName = domain.firstName,
            firstImageUri = domain.firstImageUri,
            firstInitials = domain.firstName.getInitials(),
            secondLookupKey = domain.secondLookupKey,
            secondName = domain.secondName,
            secondImageUri = domain.secondImageUri,
            secondInitials = domain.secondName.getInitials()
        )
    }
}
