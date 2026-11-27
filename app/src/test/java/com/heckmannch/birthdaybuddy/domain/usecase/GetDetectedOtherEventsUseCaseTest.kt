package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [GetDetectedOtherEventsUseCase].
 */
class GetDetectedOtherEventsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var useCase: GetDetectedOtherEventsUseCase

    @Before
    fun setUp() {
        useCase = GetDetectedOtherEventsUseCase(mainDispatcherRule.testDispatcher)
    }

    @Test
    fun detect_returnsEmptyListWhenNoOtherEventsExist() {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10),
            )
        )

        val result = GetDetectedOtherEventsUseCase.detect(contacts)

        assertThat(result).isEmpty()
    }

    @Test
    fun detect_returnsCanonicalOrderWithAccurateCountsAndDisabledFlags() = runTest {
        val jahrestag = ContactLabels.customEventLabel("Jahrestag")
        val einschulung = ContactLabels.customEventLabel("einschulung")

        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                anniversary = LocalDate.of(2018, 6, 15),
                nameDay = LocalDate.of(2020, 7, 26),
                customEvents = mapOf(
                    jahrestag to LocalDate.of(2019, 3, 1),
                    ContactLabels.LABEL_OTHER_EVENT to LocalDate.of(2021, 9, 10),
                ),
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Bob",
                anniversary = LocalDate.of(2018, 6, 15),
                customEvents = mapOf(
                    jahrestag to LocalDate.of(2019, 3, 1),
                    einschulung to LocalDate.of(2024, 9, 2),
                ),
            ),
        )

        val disabled = setOf(ContactLabels.LABEL_NAME_DAY, jahrestag)

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            disabledEventKeys = MutableStateFlow(disabled),
        ).first()

        assertThat(result).containsExactly(
            DetectedOtherEvent(
                eventKey = ContactLabels.LABEL_ANNIVERSARY,
                contactCount = 2,
                isEnabled = true,
            ),
            DetectedOtherEvent(
                eventKey = ContactLabels.LABEL_NAME_DAY,
                contactCount = 1,
                isEnabled = false,
            ),
            DetectedOtherEvent(
                eventKey = einschulung,
                contactCount = 1,
                isEnabled = true,
            ),
            DetectedOtherEvent(
                eventKey = jahrestag,
                contactCount = 2,
                isEnabled = false,
            ),
            DetectedOtherEvent(
                eventKey = ContactLabels.LABEL_OTHER_EVENT,
                contactCount = 1,
                isEnabled = true,
            ),
        ).inOrder()
    }
}
