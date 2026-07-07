package com.heckmannch.birthdaybuddy.ui.screens.settings.labels

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.local.ContactLabels
import com.heckmannch.birthdaybuddy.data.local.LabelConfig
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LabelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mock()

    @Test
    fun `labelManagementList should combine configs and contacts correctly`() = runTest {
        // Given
        val configs = listOf(
            LabelConfig("Family", isHiddenFromFilter = false, isIgnored = false),
            LabelConfig("Friends", isHiddenFromFilter = true, isIgnored = false),
            LabelConfig(
                ContactLabels.LABEL_NO_BIRTHDAY,
                isHiddenFromFilter = false,
                isIgnored = true
            )
        )
        val contacts = listOf(
            Contact(
                contactId = "1", lookupKey = "k1", fullName = "A",
                labels = listOf("Family"), birthday = LocalDate.now()
            ),
            Contact(
                contactId = "2", lookupKey = "k2", fullName = "B",
                labels = listOf("Friends"), birthday = null
            )
        )

        whenever(contactRepository.labelConfigs).thenReturn(flowOf(configs))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))

        val viewModel = LabelViewModel(contactRepository)

        // When
        val list = viewModel.labelManagementList.first { it.size == 3 }

        // Then
        assertThat(list).hasSize(3)
        assertThat(list[0].name).isEqualTo("Family")
        assertThat(list[1].name).isEqualTo("Friends")
        assertThat(list[2].name).isEqualTo(ContactLabels.LABEL_NO_BIRTHDAY)
        assertThat(list[2].isIgnored).isTrue()
    }

    @Test
    fun `updateLabelConfig should delegate to repository`() = runTest {
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.allContacts).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        val viewModel = LabelViewModel(contactRepository)

        // When
        viewModel.updateLabelConfig("Test", hidden = true, ignored = false, isSystem = true)

        // Then
        verify(contactRepository).updateLabelConfig(any())
    }

    @Test
    fun `setLabelsEnabled should delegate to repository`() = runTest {
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.allContacts).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        val viewModel = LabelViewModel(contactRepository)

        // When
        viewModel.setLabelsEnabled(false)

        // Then
        verify(contactRepository).updateLabelsEnabled(false)
    }
}
