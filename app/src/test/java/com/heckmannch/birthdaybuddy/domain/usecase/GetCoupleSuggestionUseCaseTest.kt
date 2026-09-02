package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.PotentialCouple
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetCoupleSuggestionUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mock()
    private lateinit var useCase: GetCoupleSuggestionUseCase

    private val potentialCouplesFlow = MutableStateFlow<List<PotentialCouple>>(emptyList())
    private val ignoredCouplePairsFlow = MutableStateFlow<List<String>>(emptyList())

    @Before
    fun setUp() {
        whenever(contactRepository.potentialCouples).thenReturn(potentialCouplesFlow)
        whenever(contactRepository.ignoredCouplePairs).thenReturn(ignoredCouplePairsFlow)
        useCase = GetCoupleSuggestionUseCase(contactRepository)
    }

    @Test
    fun `when selected label is not anniversary, returns null`() = runTest {
        // Arrange
        val couple =
            PotentialCouple("key1", "Max Mustermann", null, "key2", "Erika Mustermann", null)
        potentialCouplesFlow.value = listOf(couple)

        // Act
        val result = useCase(MutableStateFlow("SOME_OTHER_LABEL")).first()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `when potentials list is empty, returns null`() = runTest {
        // Arrange
        potentialCouplesFlow.value = emptyList()

        // Act
        val result = useCase(MutableStateFlow(ContactLabels.LABEL_ANNIVERSARY)).first()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `when suggestion is in ignored list, returns null`() = runTest {
        // Arrange
        val couple =
            PotentialCouple("key1", "Max Mustermann", null, "key2", "Erika Mustermann", null)
        potentialCouplesFlow.value = listOf(couple)
        ignoredCouplePairsFlow.value = listOf("key1:key2") // Ignored pair

        // Act
        val result = useCase(MutableStateFlow(ContactLabels.LABEL_ANNIVERSARY)).first()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `when suggestion is not ignored, returns suggestion mapping`() = runTest {
        // Arrange
        val couple =
            PotentialCouple("key1", "Max Mustermann", null, "key2", "Erika Mustermann", null)
        potentialCouplesFlow.value = listOf(couple)
        ignoredCouplePairsFlow.value = emptyList()

        // Act
        val result = useCase(MutableStateFlow(ContactLabels.LABEL_ANNIVERSARY)).first()

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.firstLookupKey).isEqualTo("key1")
        assertThat(result.firstName).isEqualTo("Max Mustermann")
        assertThat(result.secondLookupKey).isEqualTo("key2")
        assertThat(result.secondName).isEqualTo("Erika Mustermann")
    }

    @Test
    fun `when potential couple has different last names, returns suggestion successfully`() =
        runTest {
            // Arrange
            val couple = PotentialCouple(
                firstLookupKey = "key1",
                firstName = "Max Schmidt",
                firstImageUri = null,
                secondLookupKey = "key2",
                secondName = "Erika Müller",
                secondImageUri = null
            )
            potentialCouplesFlow.value = listOf(couple)
            ignoredCouplePairsFlow.value = emptyList()

            // Act
            val result = useCase(MutableStateFlow(ContactLabels.LABEL_ANNIVERSARY)).first()

            // Assert
            assertThat(result).isNotNull()
            assertThat(result!!.firstLookupKey).isEqualTo("key1")
            assertThat(result.firstName).isEqualTo("Max Schmidt")
            assertThat(result.secondLookupKey).isEqualTo("key2")
            assertThat(result.secondName).isEqualTo("Erika Müller")
        }
}
