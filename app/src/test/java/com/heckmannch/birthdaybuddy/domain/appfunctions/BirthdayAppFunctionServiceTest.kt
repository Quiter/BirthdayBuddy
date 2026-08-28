package com.heckmannch.birthdaybuddy.domain.appfunctions

import androidx.appfunctions.AppFunctionInvalidArgumentException
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [BirthdayAppFunctionService] query functions.
 *
 * These tests verify the pure data-transformation logic (filtering, mapping, error handling)
 * without requiring a real [ContactRepository] or Android system services.
 *
 * Note: Only the two query functions ([BirthdayAppFunctionService.getUpcomingBirthdays] and
 * [BirthdayAppFunctionService.getContactBirthday]) are unit-testable in isolation because
 * [BirthdayAppFunctionService.sendBirthdayMessage] and [BirthdayAppFunctionService.addBirthdayToContact]
 * depend on [android.app.PendingIntent] and [android.content.Context], which require an
 * Android environment. Those functions are covered by integration / instrumented tests.
 */
class BirthdayAppFunctionServiceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mockk()

    /**
     * Concrete test subclass of the abstract [BirthdayAppFunctionService].
     * Injects the mocked [ContactRepository] and test dispatcher directly,
     * bypassing the Hilt injection that is not available in JVM unit tests.
     */
    private inner class TestBirthdayAppFunctionService : BirthdayAppFunctionService() {
        init {
            contactRepository = this@BirthdayAppFunctionServiceTest.contactRepository
            ioDispatcher = mainDispatcherRule.testDispatcher
        }
    }

    private lateinit var service: TestBirthdayAppFunctionService

    // Representative test contacts with various birthday scenarios
    private val today: LocalDate = LocalDate.now()

    private fun contactWithBirthdayInDays(
        lookupKey: String,
        name: String,
        daysFromNow: Int,
        year: Int? = 1990,
        phone: String? = "+4917612345678",
    ): Contact {
        val date = today.plusDays(daysFromNow.toLong())
        // Store birthday as a date in a sentinel year if no year given, else use actual year
        val birthdayDate = if (year != null) {
            LocalDate.of(year, date.monthValue, date.dayOfMonth)
        } else {
            LocalDate.of(1900, date.monthValue, date.dayOfMonth) // NO_YEAR_MARKER
        }
        return Contact(
            contactId = lookupKey,
            lookupKey = lookupKey,
            fullName = name,
            birthday = birthdayDate,
            phoneNumber = phone,
        )
    }

    @Before
    fun setUp() {
        service = TestBirthdayAppFunctionService()
    }

    // ---- getUpcomingBirthdays ----------------------------------------------------------------

    @Test
    fun `getUpcomingBirthdays returns contacts whose birthdays fall within the window`() = runTest {
        val contact5Days = contactWithBirthdayInDays("key1", "Anna Schmidt", daysFromNow = 5)
        val contact29Days = contactWithBirthdayInDays("key2", "Bob Müller", daysFromNow = 29)
        val contact31Days = contactWithBirthdayInDays("key3", "Carol Klein", daysFromNow = 31)

        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(
            contact5Days,
            contact29Days,
            contact31Days,
        )

        val result = service.getUpcomingBirthdays(withinDays = 30)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.contactId }).containsExactly("key1", "key2").inOrder()
        assertThat(result[0].daysUntil).isEqualTo(5)
        assertThat(result[1].daysUntil).isEqualTo(29)
    }

    @Test
    fun `getUpcomingBirthdays includes contacts whose birthday is today`() = runTest {
        val todayContact = contactWithBirthdayInDays("key_today", "Diana Huber", daysFromNow = 0)
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(todayContact)

        val result = service.getUpcomingBirthdays(withinDays = 7)

        assertThat(result).hasSize(1)
        assertThat(result[0].daysUntil).isEqualTo(0)
    }

    @Test
    fun `getUpcomingBirthdays excludes contacts outside the window`() = runTest {
        val futureContact = contactWithBirthdayInDays("key_far", "Ernst Weber", daysFromNow = 365)
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(futureContact)

        val result = service.getUpcomingBirthdays(withinDays = 30)

        assertThat(result).isEmpty()
    }

    @Test
    fun `getUpcomingBirthdays clamps withinDays below 1 to 1`() = runTest {
        val todayContact = contactWithBirthdayInDays("key_today", "Franz Bauer", daysFromNow = 0)
        val tomorrowContact = contactWithBirthdayInDays("key_tmrw", "Gabi Wolf", daysFromNow = 1)
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(
            todayContact,
            tomorrowContact,
        )

        // withinDays = 0 is invalid; clamped to 1
        val result = service.getUpcomingBirthdays(withinDays = 0)

        // Only today's contact (daysUntil = 0) and tomorrow's (daysUntil = 1) within range [0,1]
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getUpcomingBirthdays maps year to null when NO_YEAR_MARKER`() = runTest {
        val noYearContact = contactWithBirthdayInDays("key_ny", "Heidi Lang", daysFromNow = 3, year = null)
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(noYearContact)

        val result = service.getUpcomingBirthdays(withinDays = 30)

        assertThat(result).hasSize(1)
        assertThat(result[0].birthdayYear).isNull()
        assertThat(result[0].age).isNull()
    }

    @Test
    fun `getUpcomingBirthdays returns empty list when no contacts have birthdays`() = runTest {
        val noDateContact = Contact(
            contactId = "no_bday",
            lookupKey = "no_bday",
            fullName = "Iris Schulz",
            birthday = null,
        )
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(noDateContact)

        val result = service.getUpcomingBirthdays(withinDays = 30)

        assertThat(result).isEmpty()
    }

    @Test
    fun `getUpcomingBirthdays results are sorted by daysUntil ascending`() = runTest {
        val c10 = contactWithBirthdayInDays("k10", "Jan Fischer", daysFromNow = 10)
        val c1 = contactWithBirthdayInDays("k1", "Karl Braun", daysFromNow = 1)
        val c5 = contactWithBirthdayInDays("k5", "Laura Vogel", daysFromNow = 5)
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(c10, c1, c5)

        val result = service.getUpcomingBirthdays(withinDays = 30)

        assertThat(result.map { it.daysUntil }).isInOrder()
    }

    // ---- getContactBirthday -----------------------------------------------------------------

    @Test
    fun `getContactBirthday returns contact for exact name match`() = runTest {
        val contact = contactWithBirthdayInDays("key_m", "Maria Meier", daysFromNow = 10)
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(contact)

        val result = service.getContactBirthday("Maria Meier")

        assertThat(result).isNotNull()
        assertThat(result!!.fullName).isEqualTo("Maria Meier")
        assertThat(result.contactId).isEqualTo("key_m")
    }

    @Test
    fun `getContactBirthday matches partial name case-insensitively`() = runTest {
        val contact = contactWithBirthdayInDays("key_p", "Petra Richter", daysFromNow = 20)
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(contact)

        val result = service.getContactBirthday("petra")

        assertThat(result).isNotNull()
        assertThat(result!!.fullName).isEqualTo("Petra Richter")
    }

    @Test
    fun `getContactBirthday returns null when no match found`() = runTest {
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(
            contactWithBirthdayInDays("key_r", "Robin Hood", daysFromNow = 5),
        )

        val result = service.getContactBirthday("Zaphod Beeblebrox")

        assertThat(result).isNull()
    }

    @Test
    fun `getContactBirthday returns null birthday fields when contact has no birthday`() = runTest {
        val noBirthdayContact = Contact(
            contactId = "key_nb",
            lookupKey = "key_nb",
            fullName = "Stefan König",
            birthday = null,
        )
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(noBirthdayContact)

        val result = service.getContactBirthday("Stefan")

        assertThat(result).isNotNull()
        assertThat(result!!.birthdayMonth).isNull()
        assertThat(result.birthdayDay).isNull()
        assertThat(result.birthdayYear).isNull()
        assertThat(result.daysUntil).isNull()
        assertThat(result.age).isNull()
    }

    @Test
    fun `getContactBirthday throws InvalidArgumentException for blank name`() = runTest {
        coEvery { contactRepository.getAllContactsImmediate() } returns emptyList()

        var caught: AppFunctionInvalidArgumentException? = null
        try {
            service.getContactBirthday("   ")
        } catch (e: AppFunctionInvalidArgumentException) {
            caught = e
        }

        assertThat(caught).isNotNull()
        assertThat(caught).isInstanceOf(AppFunctionInvalidArgumentException::class.java)
    }

    @Test
    fun `getContactBirthday returns first alphabetical match when multiple contacts match`() = runTest {
        val andreaM = contactWithBirthdayInDays("key_am", "Andrea Müller", daysFromNow = 3)
        val andreaS = contactWithBirthdayInDays("key_as", "Andrea Schulz", daysFromNow = 10)
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(andreaS, andreaM)

        val result = service.getContactBirthday("Andrea")

        // "Andrea Müller" < "Andrea Schulz" alphabetically
        assertThat(result!!.fullName).isEqualTo("Andrea Müller")
    }
}
