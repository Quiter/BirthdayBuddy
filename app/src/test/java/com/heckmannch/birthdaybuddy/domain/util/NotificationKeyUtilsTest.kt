package com.heckmannch.birthdaybuddy.domain.util

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.EventType
import org.junit.Test

class NotificationKeyUtilsTest {

    @Test
    fun `encodeKey formats keys with appropriate prefix based on EventType`() {
        assertThat(NotificationKeyUtils.encodeKey("lookup123", EventType.BIRTHDAY))
            .isEqualTo("lookup123")
        assertThat(NotificationKeyUtils.encodeKey("lookup123", EventType.ANNIVERSARY))
            .isEqualTo("anniversary:lookup123")
        assertThat(NotificationKeyUtils.encodeKey("lookup123", EventType.NAME_DAY))
            .isEqualTo("nameday:lookup123")
    }

    @Test
    fun `encodeKey preserves lookup keys containing colons and special characters`() {
        val complexKey = "lookup:with:colons_and-special!@#$"
        assertThat(NotificationKeyUtils.encodeKey(complexKey, EventType.ANNIVERSARY))
            .isEqualTo("anniversary:lookup:with:colons_and-special!@#$")
        assertThat(NotificationKeyUtils.encodeKey(complexKey, EventType.NAME_DAY))
            .isEqualTo("nameday:lookup:with:colons_and-special!@#$")
        assertThat(NotificationKeyUtils.encodeKey(complexKey, EventType.BIRTHDAY))
            .isEqualTo(complexKey)
    }

    @Test
    fun `extractRawKey removes only known prefixes`() {
        assertThat(NotificationKeyUtils.extractRawKey("lookup123"))
            .isEqualTo("lookup123")
        assertThat(NotificationKeyUtils.extractRawKey("anniversary:lookup123"))
            .isEqualTo("lookup123")
        assertThat(NotificationKeyUtils.extractRawKey("nameday:lookup123"))
            .isEqualTo("lookup123")
    }

    @Test
    fun `extractRawKey correctly handles keys containing additional colons`() {
        // Ensuring substringAfter(":") bug is fixed
        val keyWithColons = "0r1-4234:5678:90"
        assertThat(NotificationKeyUtils.extractRawKey(keyWithColons))
            .isEqualTo("0r1-4234:5678:90")
        assertThat(NotificationKeyUtils.extractRawKey("anniversary:$keyWithColons"))
            .isEqualTo("0r1-4234:5678:90")
        assertThat(NotificationKeyUtils.extractRawKey("nameday:$keyWithColons"))
            .isEqualTo("0r1-4234:5678:90")
    }

    @Test
    fun `extractRawKey ignores unknown prefix before colon`() {
        val unknownPrefixKey = "custom:lookup123"
        assertThat(NotificationKeyUtils.extractRawKey(unknownPrefixKey))
            .isEqualTo("custom:lookup123")
    }

    @Test
    fun `extractEventType identifies event type from prefix`() {
        assertThat(NotificationKeyUtils.extractEventType("lookup123"))
            .isEqualTo(EventType.BIRTHDAY)
        assertThat(NotificationKeyUtils.extractEventType("anniversary:lookup123"))
            .isEqualTo(EventType.ANNIVERSARY)
        assertThat(NotificationKeyUtils.extractEventType("nameday:lookup123"))
            .isEqualTo(EventType.NAME_DAY)
        assertThat(NotificationKeyUtils.extractEventType("unknown:prefix:lookup123"))
            .isEqualTo(EventType.BIRTHDAY)
        assertThat(NotificationKeyUtils.extractEventType("anniversary:lookup:with:colons"))
            .isEqualTo(EventType.ANNIVERSARY)
        assertThat(NotificationKeyUtils.extractEventType("nameday:lookup:with:colons"))
            .isEqualTo(EventType.NAME_DAY)
    }
}
