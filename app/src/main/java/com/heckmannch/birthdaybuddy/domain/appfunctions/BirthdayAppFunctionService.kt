package com.heckmannch.birthdaybuddy.domain.appfunctions

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.appfunctions.model.ContactBirthday
import com.heckmannch.birthdaybuddy.domain.appfunctions.model.UpcomingBirthday
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.util.IntentExtras
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.util.hasYear
import com.heckmannch.birthdaybuddy.util.safeDaysUntilNext
import com.heckmannch.birthdaybuddy.util.safeNextAge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * AppFunction service for BirthdayBuddy, exposing birthday-related workflows to the Android
 * system, Google Assistant, and on-device AI agents.
 *
 * This app manages birthday, anniversary, and name-day reminders for your contacts.
 *
 * Operational patterns:
 * - Call `getUpcomingBirthdays` or `getContactBirthday` first to obtain a valid `contactId`
 *   before passing it to `sendBirthdayMessage` or `addBirthdayToContact`.
 * - `sendBirthdayMessage` opens a third-party messaging app; the user may need to confirm
 *   the action in that app.
 * - `addBirthdayToContact` returns a PendingIntent that opens the in-app edit screen.
 *   The user confirms the birthday change inside BirthdayBuddy.
 *
 * Constraints:
 * - `withinDays` in `getUpcomingBirthdays` is clamped to the range [1, 365].
 * - `contactName` in `getContactBirthday` uses a case-insensitive substring match.
 * - Supported values for the `app` parameter in `sendBirthdayMessage` are:
 *   `"whatsapp"`, `"signal"`, `"telegram"`, `"sms"`.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "BirthdayBuddyGeneratedAppFunctionService",
    appFunctionXmlFileName = "birthday_app_function_service",
)
abstract class BirthdayAppFunctionService : AppFunctionService() {

    @Inject
    internal lateinit var contactRepository: ContactRepository

    @Inject
    @IoDispatcher
    internal lateinit var ioDispatcher: CoroutineDispatcher

    // -----------------------------------------------------------------------------------------
    // getUpcomingBirthdays
    // -----------------------------------------------------------------------------------------

    /**
     * Returns a list of contacts whose birthdays fall within the next `withinDays` days
     * (including today).
     *
     * Required workflow: Use the returned `contactId` field with `sendBirthdayMessage` or
     * `addBirthdayToContact` in follow-up calls.
     *
     * @param withinDays Number of days to look ahead (inclusive). Clamped to 1–365.
     *   Defaults to 30.
     * @return A list of [UpcomingBirthday] objects sorted by `daysUntil` ascending,
     *   or an empty list if no birthdays fall within the window.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getUpcomingBirthdays(withinDays: Int = 30): List<UpcomingBirthday> =
        withContext(ioDispatcher) {
            val clampedDays = withinDays.coerceIn(1, 365)
            val today = LocalDate.now()
            contactRepository.getAllContactsImmediate()
                .filter { contact ->
                    val birthday = contact.birthday ?: return@filter false
                    val days = birthday.safeDaysUntilNext(today)
                    days in 0..clampedDays.toLong()
                }
                .map { contact ->
                    val birthday = contact.birthday!!
                    val days = birthday.safeDaysUntilNext(today).toInt()
                    UpcomingBirthday(
                        contactId = contact.lookupKey,
                        fullName = contact.fullName,
                        birthdayMonth = birthday.monthValue,
                        birthdayDay = birthday.dayOfMonth,
                        birthdayYear = if (birthday.hasYear) birthday.year else null,
                        daysUntil = days,
                        age = birthday.safeNextAge(today),
                    )
                }
                .sortedBy { it.daysUntil }
        }

    // -----------------------------------------------------------------------------------------
    // getContactBirthday
    // -----------------------------------------------------------------------------------------

    /**
     * Returns birthday details for a single contact whose name contains the supplied string.
     *
     * The match is case-insensitive and uses substring matching, so partial names like "anna"
     * will match "Anna Schmidt". When multiple contacts match, the first alphabetical match is
     * returned.
     *
     * @param contactName Full or partial name of the contact to search for. Must not be blank.
     * @return A [ContactBirthday] for the first matching contact, or null if no contact was
     *   found. The birthday fields (`birthdayMonth`, `birthdayDay`, etc.) are null if the
     *   matched contact has no birthday stored.
     * @throws AppFunctionInvalidArgumentException if `contactName` is blank.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getContactBirthday(contactName: String): ContactBirthday? =
        withContext(ioDispatcher) {
            if (contactName.isBlank()) {
                throw AppFunctionInvalidArgumentException(
                    errorMessage = "contactName must not be blank.",
                )
            }
            val today = LocalDate.now()
            val contact = contactRepository.getAllContactsImmediate()
                .filter { it.fullName.contains(contactName, ignoreCase = true) }
                .minByOrNull { it.fullName }
                ?: return@withContext null

            val birthday = contact.birthday
            ContactBirthday(
                contactId = contact.lookupKey,
                fullName = contact.fullName,
                birthdayMonth = birthday?.monthValue,
                birthdayDay = birthday?.dayOfMonth,
                birthdayYear = birthday?.let { if (it.hasYear) it.year else null },
                daysUntil = birthday?.safeDaysUntilNext(today)?.toInt(),
                age = birthday?.safeNextAge(today),
            )
        }

    // -----------------------------------------------------------------------------------------
    // sendBirthdayMessage
    // -----------------------------------------------------------------------------------------

    /**
     * Returns a [PendingIntent] that opens a messaging app pre-addressed to the contact,
     * allowing the user to send a birthday message without navigating through the app UI.
     *
     * Required workflow: Obtain a valid `contactId` from `getUpcomingBirthdays` or
     * `getContactBirthday` before calling this function.
     *
     * @param contactId Unique contact lookup key obtained from a prior `getUpcomingBirthdays`
     *   or `getContactBirthday` call.
     * @param app Messaging app to open. Must be one of (case-insensitive):
     *   `"whatsapp"`, `"signal"`, `"telegram"`, `"sms"`. This parameter is required.
     * @return A [PendingIntent] that launches the specified messaging app addressed to the
     *   contact's phone number.
     * @throws AppFunctionInvalidArgumentException if `contactId` does not match any
     *   stored contact, the contact has no phone number, or `app` is not a recognised value.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun sendBirthdayMessage(
        contactId: String,
        app: String,
    ): PendingIntent = withContext(ioDispatcher) {
        val contact = contactRepository.getAllContactsImmediate()
            .find { it.lookupKey == contactId }
            ?: throw AppFunctionInvalidArgumentException(
                errorMessage = "No contact found for contactId '$contactId'. " +
                        "Obtain a valid contactId from getUpcomingBirthdays or getContactBirthday.",
            )

        val phone = contact.phoneNumber
            ?: throw AppFunctionInvalidArgumentException(
                errorMessage = "Contact '${contact.fullName}' has no phone number stored.",
            )

        val intent: Intent = when (app.lowercase()) {
            "whatsapp" -> Intent(Intent.ACTION_VIEW).apply {
                data = "https://wa.me/${phone.filter { it.isDigit() }}".toUri()
                setPackage("com.whatsapp")
            }
            "signal" -> Intent(Intent.ACTION_VIEW).apply {
                data = "sgnl://send?phone=${Uri.encode(phone)}".toUri()
            }
            "telegram" -> Intent(Intent.ACTION_VIEW).apply {
                data = "tg://msg?to=${Uri.encode(phone)}".toUri()
            }
            "sms" -> Intent(Intent.ACTION_VIEW).apply {
                data = "sms:${phone.filter { it.isDigit() }}".toUri()
            }
            else -> throw AppFunctionInvalidArgumentException(
                errorMessage = "Unsupported app '$app'. Valid values: whatsapp, signal, telegram, sms.",
            )
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        PendingIntent.getActivity(
            this@BirthdayAppFunctionService,
            contactId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // -----------------------------------------------------------------------------------------
    // addBirthdayToContact
    // -----------------------------------------------------------------------------------------

    /**
     * Returns a [PendingIntent] that opens the BirthdayBuddy edit screen for the specified
     * contact, pre-filled with the supplied birthday date. The user confirms the change inside
     * the app.
     *
     * This function does not write data directly. User confirmation via the app UI is required
     * to protect the integrity of the contacts database.
     *
     * Required workflow: Obtain a valid `contactId` from `getUpcomingBirthdays` or
     * `getContactBirthday` before calling this function.
     *
     * @param contactId Unique contact lookup key obtained from a prior call.
     * @param year Four-digit birth year, or null to leave the year unset.
     * @param month Month of the birthday (1 = January, 12 = December). Must be in 1–12.
     * @param day Day of the birthday (1–31). Must be a valid day for the given month.
     * @return A [PendingIntent] that opens the contact's edit screen inside BirthdayBuddy.
     * @throws AppFunctionInvalidArgumentException if the contact is not found or
     *   the date values are out of range.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addBirthdayToContact(
        contactId: String,
        year: Int?,
        month: Int,
        day: Int,
    ): PendingIntent = withContext(ioDispatcher) {
        if (month !in 1..12) {
            throw AppFunctionInvalidArgumentException(
                errorMessage = "month must be between 1 and 12, got $month.",
            )
        }
        if (day !in 1..31) {
            throw AppFunctionInvalidArgumentException(
                errorMessage = "day must be between 1 and 31, got $day.",
            )
        }

        val contact = contactRepository.getAllContactsImmediate()
            .find { it.lookupKey == contactId }
            ?: throw AppFunctionInvalidArgumentException(
                errorMessage = "No contact found for contactId '$contactId'. " +
                        "Obtain a valid contactId from getUpcomingBirthdays or getContactBirthday.",
            )

        // Deep-link into MainActivity with the contact's ID so the edit screen opens.
        // MainActivity resolves APPFN_CONTACT_ID and APPFN_BIRTHDAY_* to pre-fill the picker.
        val intent = Intent(this@BirthdayAppFunctionService, Class.forName(
            "com.heckmannch.birthdaybuddy.MainActivity"
        )).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(IntentExtras.APPFN_CONTACT_ID, contactId)
            putExtra(IntentExtras.APPFN_CONTACT_NAME, contact.fullName)
            putExtra(IntentExtras.APPFN_BIRTHDAY_YEAR, year ?: NO_YEAR_MARKER)
            putExtra(IntentExtras.APPFN_BIRTHDAY_MONTH, month)
            putExtra(IntentExtras.APPFN_BIRTHDAY_DAY, day)
        }

        PendingIntent.getActivity(
            this@BirthdayAppFunctionService,
            contactId.hashCode() xor 0xFF,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
