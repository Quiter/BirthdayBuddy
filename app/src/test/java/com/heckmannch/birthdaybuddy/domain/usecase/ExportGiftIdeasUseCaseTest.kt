package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ExportGiftIdeasUseCaseTest {

    private val contactRepository: ContactRepository = mock()
    private val uriString: String = "content://com.android.providers.downloads.documents/document/123"
    private lateinit var useCase: ExportGiftIdeasUseCase

    @Before
    fun setUp() {
        useCase = ExportGiftIdeasUseCase(contactRepository)
    }

    @Test
    fun `invoke should delegate export call to repository`() = runTest {
        // Act
        useCase(uriString)

        // Assert
        verify(contactRepository).exportGiftIdeas(uriString)
    }
}
