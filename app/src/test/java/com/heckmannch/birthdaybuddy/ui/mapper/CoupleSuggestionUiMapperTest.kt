package com.heckmannch.birthdaybuddy.ui.mapper

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.CoupleSuggestion
import org.junit.Test

/**
 * Unit tests for [CoupleSuggestionUiMapper].
 * Verifies mapping from domain [CoupleSuggestion] to [com.heckmannch.birthdaybuddy.ui.model.CoupleSuggestionUiModel],
 * specifically ensuring that avatar initials are calculated correctly.
 */
class CoupleSuggestionUiMapperTest {

    private val mapper = CoupleSuggestionUiMapper()

    @Test
    fun toUiModel_mapsFieldsAndCalculatesInitialsCorrectly() {
        val domain = CoupleSuggestion(
            firstLookupKey = "key1",
            firstName = "Max Schmidt",
            firstImageUri = "content://image1",
            secondLookupKey = "key2",
            secondName = "Erika Müller",
            secondImageUri = null
        )

        val uiModel = mapper.toUiModel(domain)

        assertThat(uiModel.firstLookupKey).isEqualTo("key1")
        assertThat(uiModel.firstName).isEqualTo("Max Schmidt")
        assertThat(uiModel.firstImageUri).isEqualTo("content://image1")
        assertThat(uiModel.firstInitials).isEqualTo("MS")
        assertThat(uiModel.secondLookupKey).isEqualTo("key2")
        assertThat(uiModel.secondName).isEqualTo("Erika Müller")
        assertThat(uiModel.secondImageUri).isNull()
        assertThat(uiModel.secondInitials).isEqualTo("EM")
    }

    @Test
    fun toUiModel_handlesSingleNameAndEmptyNameInitials() {
        val domain = CoupleSuggestion(
            firstLookupKey = "key1",
            firstName = "Madonna",
            firstImageUri = null,
            secondLookupKey = "key2",
            secondName = "  ",
            secondImageUri = null
        )

        val uiModel = mapper.toUiModel(domain)

        assertThat(uiModel.firstInitials).isEqualTo("M")
        assertThat(uiModel.secondInitials).isEqualTo("?")
    }
}
