package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContactImageCacheKeyTest {

    @Test
    fun getAvatarCacheKey_withLookupKey_returnsCombinedKey() {
        val uri = "content://com.android.contacts/display_photo/1"
        val lookupKey = "0r1-456789"

        val key = getAvatarCacheKey(uri, lookupKey)

        assertThat(key).isEqualTo("avatar_0r1-456789_content://com.android.contacts/display_photo/1")
    }

    @Test
    fun getAvatarCacheKey_withoutLookupKey_returnsUriBasedKey() {
        val uri = "content://com.android.contacts/display_photo/2"

        val keyWithNull = getAvatarCacheKey(uri, null)
        val keyWithBlank = getAvatarCacheKey(uri, "  ")

        assertThat(keyWithNull).isEqualTo("avatar_content://com.android.contacts/display_photo/2")
        assertThat(keyWithBlank).isEqualTo("avatar_content://com.android.contacts/display_photo/2")
    }

    @Test
    fun getAvatarCacheKey_isDeterministic() {
        val uri = "content://com.android.contacts/display_photo/3"
        val lookupKey = "lookup_xyz"

        val key1 = getAvatarCacheKey(uri, lookupKey)
        val key2 = getAvatarCacheKey(uri, lookupKey)

        assertThat(key1).isEqualTo(key2)
    }
}
