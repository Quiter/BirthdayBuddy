package com.heckmannch.birthdaybuddy.data.util

import com.heckmannch.birthdaybuddy.domain.util.DeviceRegionProvider
import java.util.Locale
import javax.inject.Inject

/**
 * Android implementation of [DeviceRegionProvider] that determines the device's
 * current ISO country code via [Locale.getDefault].
 */
class AndroidDeviceRegionProvider @Inject constructor() : DeviceRegionProvider {

    override fun getCountryIso(): String = Locale.getDefault().country
}
