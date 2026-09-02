package com.heckmannch.birthdaybuddy.domain.util

import com.heckmannch.birthdaybuddy.domain.model.EventType

/**
 * Utility functions for encoding and decoding event types within notification lookup keys.
 */
object NotificationKeyUtils {
    private const val PREFIX_ANNIVERSARY = "anniversary:"
    private const val PREFIX_NAMEDAY = "nameday:"

    /**
     * Encodes a contact lookup key with an event type prefix if required.
     */
    fun encodeKey(lookupKey: String, eventType: EventType): String {
        return when (eventType) {
            EventType.ANNIVERSARY -> "$PREFIX_ANNIVERSARY$lookupKey"
            EventType.NAME_DAY -> "$PREFIX_NAMEDAY$lookupKey"
            EventType.BIRTHDAY -> lookupKey
        }
    }

    /**
     * Extracts the raw lookup key by removing known event type prefixes.
     * Uses `removePrefix` to preserve colons within the underlying lookup key.
     */
    fun extractRawKey(encodedKey: String): String {
        return when {
            encodedKey.startsWith(PREFIX_ANNIVERSARY) -> encodedKey.removePrefix(PREFIX_ANNIVERSARY)
            encodedKey.startsWith(PREFIX_NAMEDAY) -> encodedKey.removePrefix(PREFIX_NAMEDAY)
            else -> encodedKey
        }
    }

    /**
     * Extracts the [EventType] from an encoded notification key based on its prefix.
     */
    fun extractEventType(encodedKey: String): EventType {
        return when {
            encodedKey.startsWith(PREFIX_ANNIVERSARY) -> EventType.ANNIVERSARY
            encodedKey.startsWith(PREFIX_NAMEDAY) -> EventType.NAME_DAY
            else -> EventType.BIRTHDAY
        }
    }
}
