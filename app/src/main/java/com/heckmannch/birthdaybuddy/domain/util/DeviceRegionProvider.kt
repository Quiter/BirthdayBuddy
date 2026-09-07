package com.heckmannch.birthdaybuddy.domain.util

/**
 * Interface providing the device's ISO country/region code (e.g. "DE", "US").
 *
 * Decouples the domain layer from global JVM state (`Locale.getDefault()`) and platform APIs,
 * ensuring deterministic behavior and reliable unit testing across environments.
 */
interface DeviceRegionProvider {
    /**
     * Returns the 2-letter ISO 3166-1 alpha-2 country code of the device region.
     */
    fun getCountryIso(): String
}
