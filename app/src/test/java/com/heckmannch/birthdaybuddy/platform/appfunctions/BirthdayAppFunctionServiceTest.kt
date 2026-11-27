package com.heckmannch.birthdaybuddy.platform.appfunctions

import android.os.CancellationSignal
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.AppFunctionInventory
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.util.DeviceRegionProvider
import com.heckmannch.birthdaybuddy.domain.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.util.Clock
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.util.function.Consumer

/**
 * Unit tests for [BirthdayAppFunctionService] query functions and intent creation.
 *
 * These tests verify the pure data-transformation logic (filtering, mapping, error handling)
 * and messenger intent resolution (including dynamic WhatsApp and WhatsApp Business package detection).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BirthdayAppFunctionServiceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()

    private class FakeClock(
        var currentDate: LocalDate = LocalDate.of(2026, 6, 15),
    ) : Clock {
        override fun currentTimeMillis(): Long =
            currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        override fun nowLocalDate(zoneId: ZoneId): LocalDate = currentDate
    }

    private val clock = FakeClock()

    /**
     * Concrete test subclass of the abstract [BirthdayAppFunctionService].
     * Injects the mocked repositories, fake clock, and test dispatcher directly,
     * bypassing the Hilt injection that is not available in JVM unit tests.
     */
    private inner class TestBirthdayAppFunctionService : BirthdayAppFunctionService() {
        val installedPackages = mutableSetOf<String>()

        init {
            contactRepository = this@BirthdayAppFunctionServiceTest.contactRepository
            settingsRepository = this@BirthdayAppFunctionServiceTest.settingsRepository
            ioDispatcher = mainDispatcherRule.testDispatcher
            deviceRegionProvider = object : DeviceRegionProvider {
                override fun getCountryIso(): String = "DE"
            }
            clock = this@BirthdayAppFunctionServiceTest.clock
        }

        override fun isPackageInstalled(pm: android.content.pm.PackageManager, packageName: String): Boolean {
            return installedPackages.contains(packageName)
        }

        override fun resolveWhatsAppPackage(): String? {
            return WHATSAPP_PACKAGES.firstOrNull { installedPackages.contains(it) }
        }

        override fun onExecuteFunction(
            request: ExecuteAppFunctionRequest,
            metadata: androidx.appfunctions.metadata.AppFunctionMetadata,
            cancellationSignal: CancellationSignal,
            callback: Consumer<ExecuteAppFunctionResponse>
        ) {
            // Not used in unit tests
        }

        override fun resolveInventory(): AppFunctionInventory = mockk(relaxed = true)
    }

    private lateinit var service: TestBirthdayAppFunctionService

    // Representative test contacts with various birthday scenarios
    private val today: LocalDate
        get() = clock.currentDate

    private fun contactWithBirthdayInDays(
        lookupKey: String,
        name: String,
        daysFromNow: Int,
        year: Int? = 1990,
        phone: String? = "+4917612345678",
        labels: List<String> = emptyList(),
    ): Contact {
        val date = today.plusDays(daysFromNow.toLong())
        // Store birthday as a date in a sentinel year if no year given, else use actual year
        val birthdayDate = if (year != null) {
            LocalDate.of(year, date.monthValue, date.dayOfMonth)
        } else {
            LocalDate.of(NO_YEAR_MARKER, date.monthValue, date.dayOfMonth)
        }
        return Contact(
            contactId = lookupKey,
            lookupKey = lookupKey,
            fullName = name,
            birthday = birthdayDate,
            phoneNumber = phone,
            labels = labels,
        )
    }

    @Before
    fun setUp() {
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(labelsEnabled = true)
        every { contactRepository.labelConfigs } returns flowOf(emptyList())
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
        val futureContact = contactWithBirthdayInDays("key_far", "Ernst Weber", daysFromNow = 100)
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
        val noYearContact =
            contactWithBirthdayInDays("key_ny", "Heidi Lang", daysFromNow = 3, year = null)
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

        val result = runCatching { service.getContactBirthday("   ") }

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getContactBirthday returns first alphabetical match when multiple contacts match`() =
        runTest {
            val andreaM = contactWithBirthdayInDays("key_am", "Andrea Müller", daysFromNow = 3)
            val andreaS = contactWithBirthdayInDays("key_as", "Andrea Schulz", daysFromNow = 10)
            coEvery { contactRepository.getAllContactsImmediate() } returns listOf(andreaS, andreaM)

            val result = service.getContactBirthday("Andrea")

            // "Andrea Müller" < "Andrea Schulz" alphabetically
            assertThat(result!!.fullName).isEqualTo("Andrea Müller")
        }

    // ---- Ignored labels & Clock injection ---------------------------------------------------

    @Test
    fun `getUpcomingBirthdays excludes contacts with ignored label when labelsEnabled is true`() = runTest {
        val visible = contactWithBirthdayInDays("k_vis", "Anna Visible", daysFromNow = 5, labels = listOf("Family"))
        val ignored = contactWithBirthdayInDays("k_ign", "Bob Ignored", daysFromNow = 5, labels = listOf("Ex-Colleagues"))

        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(visible, ignored)
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(labelsEnabled = true)
        every { contactRepository.labelConfigs } returns flowOf(
            listOf(
                LabelConfig(name = "Family", isIgnored = false),
                LabelConfig(name = "Ex-Colleagues", isIgnored = true),
            )
        )

        val result = service.getUpcomingBirthdays(withinDays = 30)

        assertThat(result.map { it.contactId }).containsExactly("k_vis")
    }

    @Test
    fun `getUpcomingBirthdays includes contacts with ignored label when labelsEnabled is false`() = runTest {
        val visible = contactWithBirthdayInDays("k_vis", "Anna Visible", daysFromNow = 5, labels = listOf("Family"))
        val ignored = contactWithBirthdayInDays("k_ign", "Bob Ignored", daysFromNow = 6, labels = listOf("Ex-Colleagues"))

        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(visible, ignored)
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(labelsEnabled = false)
        every { contactRepository.labelConfigs } returns flowOf(
            listOf(LabelConfig(name = "Ex-Colleagues", isIgnored = true))
        )

        val result = service.getUpcomingBirthdays(withinDays = 30)

        assertThat(result.map { it.contactId }).containsExactly("k_vis", "k_ign").inOrder()
    }

    @Test
    fun `getContactBirthday excludes ignored contact when labelsEnabled is true and returns it when false`() = runTest {
        val ignored = contactWithBirthdayInDays("k_ign", "Bob Ignored", daysFromNow = 5, labels = listOf("Spam"))
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(ignored)
        every { contactRepository.labelConfigs } returns flowOf(
            listOf(LabelConfig(name = "Spam", isIgnored = true))
        )

        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(labelsEnabled = true)
        assertThat(service.getContactBirthday("Bob")).isNull()

        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(labelsEnabled = false)
        assertThat(service.getContactBirthday("Bob")?.contactId).isEqualTo("k_ign")
    }

    @Test
    fun `getUpcomingBirthdays and getContactBirthday use injected clock deterministically`() = runTest {
        clock.currentDate = LocalDate.of(2026, 6, 15)
        val contact = Contact(
            contactId = "k_clk",
            lookupKey = "k_clk",
            fullName = "Clara Clock",
            birthday = LocalDate.of(1996, 6, 20),
        )
        coEvery { contactRepository.getAllContactsImmediate() } returns listOf(contact)

        val upcoming1 = service.getUpcomingBirthdays(withinDays = 30)
        val single1 = service.getContactBirthday("Clara")
        assertThat(upcoming1.single().daysUntil).isEqualTo(5)
        assertThat(upcoming1.single().age).isEqualTo(30)
        assertThat(single1?.daysUntil).isEqualTo(5)
        assertThat(single1?.age).isEqualTo(30)

        // Advance clock to birthday next year (2027-06-19 -> 1 day before 31st birthday)
        clock.currentDate = LocalDate.of(2027, 6, 19)
        val upcoming2 = service.getUpcomingBirthdays(withinDays = 30)
        val single2 = service.getContactBirthday("Clara")
        assertThat(upcoming2.single().daysUntil).isEqualTo(1)
        assertThat(upcoming2.single().age).isEqualTo(31)
        assertThat(single2?.daysUntil).isEqualTo(1)
        assertThat(single2?.age).isEqualTo(31)
    }

    // ---- buildSendBirthdayMessageIntent ----------------------------------------------------

    @Test
    fun `buildSendBirthdayMessageIntent with whatsapp sets package to com_whatsapp when installed`() {
        service.installedPackages.add("com.whatsapp")

        val intent = service.buildSendBirthdayMessageIntent(app = "whatsapp", phone = "+49 170 1234567")

        assertThat(intent.action).isEqualTo(android.content.Intent.ACTION_VIEW)
        assertThat(intent.data.toString()).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(intent.`package`).isEqualTo("com.whatsapp")
    }

    @Test
    fun `buildSendBirthdayMessageIntent with whatsapp sets package to com_whatsapp_w4b when only business installed`() {
        service.installedPackages.add("com.whatsapp.w4b")

        val intent = service.buildSendBirthdayMessageIntent(app = "whatsapp", phone = "+49 170 1234567")

        assertThat(intent.action).isEqualTo(android.content.Intent.ACTION_VIEW)
        assertThat(intent.data.toString()).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(intent.`package`).isEqualTo("com.whatsapp.w4b")
    }

    @Test
    fun `buildSendBirthdayMessageIntent with whatsapp prefers standard over business when both installed`() {
        service.installedPackages.add("com.whatsapp")
        service.installedPackages.add("com.whatsapp.w4b")

        val intent = service.buildSendBirthdayMessageIntent(app = "whatsapp", phone = "+49 170 1234567")

        assertThat(intent.`package`).isEqualTo("com.whatsapp")
    }

    @Test
    fun `buildSendBirthdayMessageIntent with whatsapp leaves package null for universal link fallback when none installed`() {
        service.installedPackages.clear()

        val intent = service.buildSendBirthdayMessageIntent(app = "whatsapp", phone = "+49 170 1234567")

        assertThat(intent.action).isEqualTo(android.content.Intent.ACTION_VIEW)
        assertThat(intent.data.toString()).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(intent.`package`).isNull()
    }

    @Test
    fun `buildSendBirthdayMessageIntent with signal generates valid uri and targets Signal package`() {
        val intent = service.buildSendBirthdayMessageIntent(app = "signal", phone = "+49 170 1234567")

        assertThat(intent.action).isEqualTo(android.content.Intent.ACTION_VIEW)
        assertThat(intent.data.toString()).isEqualTo("https://signal.me/#p/+491701234567")
        assertThat(intent.`package`).isEqualTo("org.thoughtcrime.securesms")
    }

    @Test
    fun `buildSendBirthdayMessageIntent with telegram generates valid uri with percent-encoded plus`() {
        val intent = service.buildSendBirthdayMessageIntent(app = "telegram", phone = "+49 170 1234567")

        assertThat(intent.action).isEqualTo(android.content.Intent.ACTION_VIEW)
        assertThat(intent.data.toString()).isEqualTo("tg://resolve?phone=%2B491701234567")
        assertThat(intent.data?.getQueryParameter("phone")).isEqualTo("+491701234567")
    }

    @Test
    fun `buildSendBirthdayMessageIntent with sms generates valid smsto uri`() {
        val intent = service.buildSendBirthdayMessageIntent(app = "sms", phone = "+49 170 1234567")

        assertThat(intent.action).isEqualTo(android.content.Intent.ACTION_SENDTO)
        assertThat(intent.data.toString()).isEqualTo("smsto:+491701234567")
    }

    @Test
    fun `buildSendBirthdayMessageIntent throws InvalidArgumentException for unsupported app`() {
        val result = runCatching {
            service.buildSendBirthdayMessageIntent(app = "carrier_pigeon", phone = "+49 170 1234567")
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull())
            .isInstanceOf(androidx.appfunctions.AppFunctionInvalidArgumentException::class.java)
    }
}
