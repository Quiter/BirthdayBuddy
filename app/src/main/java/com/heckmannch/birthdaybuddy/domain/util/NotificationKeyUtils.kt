package com.heckmannch.birthdaybuddy.domain.util

import com.heckmannch.birthdaybuddy.domain.model.EventType

/**
 * Utility functions for encoding and decoding event types (including custom date-field keys)
 * within notification lookup keys.
 */
object NotificationKeyUtils {
    private const val PREFIX_ANNIVERSARY = "anniversary:"
    private const val PREFIX_NAMEDAY = "nameday:"
    private const val PREFIX_CUSTOM = "custom:"

    /**
     * Encodes a contact lookup key with an event type prefix if required.
     *
     * For [EventType.CUSTOM], a length-prefixed format (`custom:<len>:<eventKey>:<lookupKey>`)
     * is used so arbitrary colons or special characters in either `eventKey` or `lookupKey`
     * are preserved losslessly without Android framework dependencies.
     */
    fun encodeKey(
        lookupKey: String,
        eventType: EventType,
        customEventKey: String? = null,
    ): String {
        return when (eventType) {
            EventType.ANNIVERSARY -> "$PREFIX_ANNIVERSARY$lookupKey"
            EventType.NAME_DAY -> "$PREFIX_NAMEDAY$lookupKey"
            EventType.CUSTOM -> if (customEventKey != null) {
                "$PREFIX_CUSTOM${customEventKey.length}:$customEventKey:$lookupKey"
            } else {
                "$PREFIX_CUSTOM$lookupKey"
            }
            EventType.BIRTHDAY -> lookupKey
        }
    }

    /**
     * Extracts the raw lookup key by removing known event type prefixes.
     * Uses `removePrefix` and length-aware parsing to preserve colons within the underlying lookup key.
     */
    fun extractRawKey(encodedKey: String): String {
        return when {
            encodedKey.startsWith(PREFIX_ANNIVERSARY) -> encodedKey.removePrefix(PREFIX_ANNIVERSARY)
            encodedKey.startsWith(PREFIX_NAMEDAY) -> encodedKey.removePrefix(PREFIX_NAMEDAY)
            encodedKey.startsWith(PREFIX_CUSTOM) -> {
                val payload = encodedKey.removePrefix(PREFIX_CUSTOM)
                parseCustomPayload(payload)?.second ?: encodedKey
            }
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
            encodedKey.startsWith(PREFIX_CUSTOM) &&
                parseCustomPayload(encodedKey.removePrefix(PREFIX_CUSTOM)) != null -> EventType.CUSTOM
            else -> EventType.BIRTHDAY
        }
    }

    /**
     * Extracts the canonical custom event key (e.g. `custom_event:Taufe` or `special:other_event`)
     * from a [EventType.CUSTOM]-encoded notification key, or `null` for non-custom keys.
     */
    fun extractCustomEventKey(encodedKey: String): String? {
        if (!encodedKey.startsWith(PREFIX_CUSTOM)) return null
        val payload = encodedKey.removePrefix(PREFIX_CUSTOM)
        return parseCustomPayload(payload)?.first
    }

    private fun parseCustomPayload(payload: String): Pair<String, String>? {
        val lenSep = payload.indexOf(':')
        if (lenSep <= 0) return null
        val keyLen = payload.substring(0, lenSep).toIntOrNull() ?: return null
        val keyStart = lenSep + 1
        val keyEnd = keyStart + keyLen
        if (keyLen < 0 || keyEnd >= payload.length || payload[keyEnd] != ':') return null
        val eventKey = payload.substring(keyStart, keyEnd)
        val rawLookupKey = payload.substring(keyEnd + 1)
        return eventKey to rawLookupKey
    }
}
