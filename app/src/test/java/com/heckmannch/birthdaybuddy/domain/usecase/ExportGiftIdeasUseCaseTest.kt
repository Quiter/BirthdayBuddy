package com.heckmannch.birthdaybuddy.domain.usecase

import android.net.Uri
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
    private val uri: Uri = mock()
    private lateinit var useCase: ExportGiftIdeasUseCase

    @Before
    fun setUp() {
        useCase = ExportGiftIdeasUseCase(contactRepository)
    }

    @Test
    fun `invoke should delegate export call to repository`() = runTest {
        // Act
        useCase(uri)

        // Assert
        verify(contactRepository).exportGiftIdeas(uri)
    }
}
