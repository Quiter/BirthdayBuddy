package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.util.hasYear
import com.heckmannch.birthdaybuddy.util.mergeNames
import com.heckmannch.birthdaybuddy.util.safeNextAge
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Formats notification titles and content descriptions for birthday, anniversary, name day,
 * and dynamic custom date-field events.
 *
 * Encapsulates string formatting logic across single contacts (with/without age), married couples,
 * and multi-contact groups.
 */
@Singleton
class NotificationTextFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Checks if the given contacts represent a married couple sharing an anniversary.
     *
     * @param contacts The list of contacts associated with the notification.
     * @param eventType The type of event (e.g., [EventType.ANNIVERSARY]).
     * @return `true` if exactly two contacts are mutually linked via spouse keys for an anniversary.
     */
    fun isCoupleAnniversary(contacts: List<Contact>, eventType: EventType): Boolean {
        return eventType == EventType.ANNIVERSARY &&
            contacts.size == 2 &&
            contacts[0].spouseLookupKey == contacts[1].lookupKey &&
            contacts[1].spouseLookupKey == contacts[0].lookupKey
    }

    /**
     * Resolves a human-readable display name for a [customEventKey].
     */
    private fun resolveCustomEventName(customEventKey: String?): String = when {
        customEventKey == null || customEventKey == ContactLabels.LABEL_OTHER_EVENT ->
            context.getString(R.string.home_filter_other_event)
        ContactLabels.isCustomEventLabel(customEventKey) ->
            ContactLabels.extractCustomEventName(customEventKey)
                ?: context.getString(R.string.home_filter_other_event)
        else -> customEventKey
    }

    /**
     * Builds the notification title based on the contacts, time offset in days, and event type.
     *
     * @param contacts The list of contacts associated with the event.
     * @param daysBefore The number of days before the event (0 = today, 1 = tomorrow, 7 = 1 week, etc.).
     * @param eventType The type of event ([EventType.BIRTHDAY], [EventType.ANNIVERSARY], [EventType.NAME_DAY], [EventType.CUSTOM]).
     * @param today The reference date for age and date calculations (defaults to [LocalDate.now]).
     * @param customEventKey Optional canonical custom event key when [eventType] is [EventType.CUSTOM].
     * @return The localized title string.
     */
    fun buildTitle(
        contacts: List<Contact>,
        daysBefore: Int,
        eventType: EventType = EventType.BIRTHDAY,
        today: LocalDate = LocalDate.now(),
        customEventKey: String? = null,
    ): String {
        if (contacts.isEmpty()) return ""

        val isCouple = isCoupleAnniversary(contacts, eventType)
        return if (contacts.size == 1 || isCouple) {
            buildSingleOrCoupleTitle(contacts, daysBefore, eventType, isCouple, today, customEventKey)
        } else {
            buildGroupTitle(contacts.size, daysBefore, eventType, customEventKey)
        }
    }

    /**
     * Builds the notification content text (subtext or body description).
     *
     * @param contacts The list of contacts associated with the event.
     * @param eventType The type of event ([EventType.BIRTHDAY], [EventType.ANNIVERSARY], [EventType.NAME_DAY], [EventType.CUSTOM]).
     * @param showHint Whether to display a hint explaining how persistent notifications work.
     * @param customEventKey Optional canonical custom event key when [eventType] is [EventType.CUSTOM].
     * @return The formatted content text.
     */
    fun buildContentText(
        contacts: List<Contact>,
        eventType: EventType = EventType.BIRTHDAY,
        showHint: Boolean = false,
        customEventKey: String? = null,
    ): String {
        val isCouple = isCoupleAnniversary(contacts, eventType)
        return if (contacts.size == 1 || isCouple) {
            val defaultDesc = when (eventType) {
                EventType.ANNIVERSARY -> context.getString(R.string.notif_desc_anniversary)
                EventType.NAME_DAY -> context.getString(R.string.notif_desc_nameday)
                EventType.CUSTOM -> context.getString(
                    R.string.notif_desc_custom,
                    resolveCustomEventName(customEventKey)
                )
                EventType.BIRTHDAY -> context.getString(R.string.notif_desc_named)
            }
            if (showHint) {
                context.getString(R.string.notif_hint_persistent)
            } else {
                defaultDesc
            }
        } else {
            val list = contacts.joinToString(", ") { it.fullName }
            if (showHint) {
                "${context.getString(R.string.notif_hint_persistent)} ($list)"
            } else {
                list
            }
        }
    }

    private fun buildSingleOrCoupleTitle(
        contacts: List<Contact>,
        daysBefore: Int,
        eventType: EventType,
        isCouple: Boolean,
        today: LocalDate,
        customEventKey: String?,
    ): String {
        val name = if (isCouple) {
            mergeNames(contacts[0].fullName, contacts[1].fullName)
        } else {
            contacts.first().fullName
        }
        val contact = contacts.first()

        return when (eventType) {
            EventType.ANNIVERSARY -> buildAnniversaryTitle(contact, name, daysBefore, isCouple, today)
            EventType.NAME_DAY -> buildNameDayTitle(name, daysBefore)
            EventType.CUSTOM -> buildCustomEventTitle(name, daysBefore, resolveCustomEventName(customEventKey))
            EventType.BIRTHDAY -> buildBirthdayTitle(contact, name, daysBefore, today)
        }
    }

    private fun buildCustomEventTitle(
        name: String,
        daysBefore: Int,
        eventName: String,
    ): String {
        return when (daysBefore) {
            0 -> context.getString(R.string.notif_title_today_custom, name, eventName)
            1 -> context.getString(R.string.notif_title_tomorrow_custom, name, eventName)
            7 -> context.getString(R.string.notif_title_week_custom, name, eventName)
            else -> context.resources.getQuantityString(
                R.plurals.notif_title_days_custom,
                daysBefore,
                daysBefore,
                name,
                eventName,
            )
        }
    }

    private fun buildAnniversaryTitle(
        contact: Contact,
        name: String,
        daysBefore: Int,
        isCouple: Boolean,
        today: LocalDate
    ): String {
        val anniversary = contact.anniversary
        val hasYear = anniversary?.hasYear ?: false
        val nextYears = anniversary?.safeNextAge(today) ?: -1

        return when (daysBefore) {
            0 -> if (hasYear) {
                context.getString(R.string.notif_title_today_anniversary_age, name, nextYears)
            } else {
                val resId = if (isCouple) {
                    R.string.notif_title_today_anniversary_couple
                } else {
                    R.string.notif_title_today_anniversary
                }
                context.getString(resId, name)
            }

            1 -> if (hasYear) {
                context.getString(R.string.notif_title_tomorrow_anniversary_age, name, nextYears)
            } else {
                val resId = if (isCouple) {
                    R.string.notif_title_tomorrow_anniversary_couple
                } else {
                    R.string.notif_title_tomorrow_anniversary
                }
                context.getString(resId, name)
            }

            7 -> if (hasYear) {
                context.getString(R.string.notif_title_week_anniversary_age, name, nextYears)
            } else {
                val resId = if (isCouple) {
                    R.string.notif_title_week_anniversary_couple
                } else {
                    R.string.notif_title_week_anniversary
                }
                context.getString(resId, name)
            }

            else -> if (hasYear) {
                context.resources.getQuantityString(
                    R.plurals.notif_title_days_anniversary_age,
                    daysBefore,
                    daysBefore,
                    name,
                    nextYears
                )
            } else {
                val pluralsId = if (isCouple) {
                    R.plurals.notif_title_days_anniversary_couple
                } else {
                    R.plurals.notif_title_days_anniversary
                }
                context.resources.getQuantityString(
                    pluralsId,
                    daysBefore,
                    daysBefore,
                    name
                )
            }
        }
    }

    private fun buildNameDayTitle(name: String, daysBefore: Int): String {
        return when (daysBefore) {
            0 -> context.getString(R.string.notif_title_today_nameday, name)
            1 -> context.getString(R.string.notif_title_tomorrow_nameday, name)
            7 -> context.getString(R.string.notif_title_week_nameday, name)
            else -> context.resources.getQuantityString(
                R.plurals.notif_title_days_nameday,
                daysBefore,
                daysBefore,
                name
            )
        }
    }

    private fun buildBirthdayTitle(
        contact: Contact,
        name: String,
        daysBefore: Int,
        today: LocalDate
    ): String {
        val birthday = contact.birthday
        val hasYear = birthday?.hasYear ?: false
        val nextAge = birthday?.safeNextAge(today) ?: -1

        return when (daysBefore) {
            0 -> if (hasYear) {
                context.resources.getQuantityString(
                    R.plurals.notif_title_today_age,
                    nextAge,
                    name,
                    nextAge
                )
            } else {
                context.getString(R.string.notif_title_today_named, name)
            }

            1 -> if (hasYear) {
                context.resources.getQuantityString(
                    R.plurals.notif_title_tomorrow_age,
                    nextAge,
                    name,
                    nextAge
                )
            } else {
                context.getString(R.string.notif_title_tomorrow_named, name)
            }

            7 -> if (hasYear) {
                context.getString(R.string.notif_title_week_age, name, nextAge)
            } else {
                context.getString(R.string.notif_title_week_named, name)
            }

            else -> if (hasYear) {
                context.resources.getQuantityString(
                    R.plurals.notif_title_days_age,
                    daysBefore,
                    daysBefore,
                    name,
                    nextAge
                )
            } else {
                context.resources.getQuantityString(
                    R.plurals.notif_title_days_named,
                    daysBefore,
                    daysBefore,
                    name
                )
            }
        }
    }

    private fun buildGroupTitle(
        contactCount: Int,
        daysBefore: Int,
        eventType: EventType,
        customEventKey: String?,
    ): String {
        return when (eventType) {
            EventType.ANNIVERSARY -> when (daysBefore) {
                0 -> context.resources.getQuantityString(
                    R.plurals.notif_title_today_anniversary_plural,
                    contactCount,
                    contactCount
                )

                1 -> context.resources.getQuantityString(
                    R.plurals.notif_title_tomorrow_anniversary_plural,
                    contactCount,
                    contactCount
                )

                7 -> context.resources.getQuantityString(
                    R.plurals.notif_title_week_anniversary_plural,
                    contactCount,
                    contactCount
                )

                else -> context.resources.getQuantityString(
                    R.plurals.notif_title_days_anniversary_plural,
                    daysBefore,
                    daysBefore,
                    contactCount
                )
            }

            EventType.NAME_DAY -> when (daysBefore) {
                0 -> context.resources.getQuantityString(
                    R.plurals.notif_title_today_nameday_plural,
                    contactCount,
                    contactCount
                )

                1 -> context.resources.getQuantityString(
                    R.plurals.notif_title_tomorrow_nameday_plural,
                    contactCount,
                    contactCount
                )

                7 -> context.resources.getQuantityString(
                    R.plurals.notif_title_week_nameday_plural,
                    contactCount,
                    contactCount
                )

                else -> context.resources.getQuantityString(
                    R.plurals.notif_title_days_nameday_plural,
                    daysBefore,
                    daysBefore,
                    contactCount
                )
            }

            EventType.CUSTOM -> {
                val eventName = resolveCustomEventName(customEventKey)
                when (daysBefore) {
                    0 -> context.resources.getQuantityString(
                        R.plurals.notif_title_today_custom_plural,
                        contactCount,
                        contactCount,
                        eventName,
                    )

                    1 -> context.resources.getQuantityString(
                        R.plurals.notif_title_tomorrow_custom_plural,
                        contactCount,
                        contactCount,
                        eventName,
                    )

                    7 -> context.resources.getQuantityString(
                        R.plurals.notif_title_week_custom_plural,
                        contactCount,
                        contactCount,
                        eventName,
                    )

                    else -> context.resources.getQuantityString(
                        R.plurals.notif_title_days_custom_plural,
                        daysBefore,
                        daysBefore,
                        contactCount,
                        eventName,
                    )
                }
            }

            EventType.BIRTHDAY -> when (daysBefore) {
                0 -> context.resources.getQuantityString(
                    R.plurals.notif_title_today_plural,
                    contactCount,
                    contactCount
                )

                1 -> context.resources.getQuantityString(
                    R.plurals.notif_title_tomorrow_plural,
                    contactCount,
                    contactCount
                )

                7 -> context.resources.getQuantityString(
                    R.plurals.notif_title_week_plural,
                    contactCount,
                    contactCount
                )

                else -> context.resources.getQuantityString(
                    R.plurals.notif_title_days_plural,
                    daysBefore,
                    daysBefore,
                    contactCount
                )
            }
        }
    }
}
