package com.heckmannch.birthdaybuddy.util

import kotlinx.serialization.json.Json

/**
 * Centralized utility providing configured [Json] serializer instances.
 *
 * Serves as the Single Source of Truth for JSON serialization throughout the application,
 * eliminating redundant Json { ... } configurations across Room TypeConverters and backup managers.
 */
object JsonUtils {

    /**
     * Default [Json] instance configured with lenient parsing and default encoding.
     *
     * - ignoreUnknownKeys = true: Ensures backward and forward compatibility when data models evolve.
     * - encodeDefaults = true: Guarantees default values in models are serialized explicitly.
     */
    val defaultJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Pretty-printed [Json] instance for user-facing exports (e.g. backup files).
     */
    val prettyJson: Json = Json(defaultJson) {
        prettyPrint = true
    }
}
