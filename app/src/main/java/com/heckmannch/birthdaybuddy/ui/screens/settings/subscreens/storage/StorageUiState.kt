package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.storage

import androidx.compose.runtime.Immutable
import java.util.Locale

/**
 * UI state for the Storage Settings screen.
 *
 * @property databaseBytes Combined size of all Room databases including sidecar files in bytes.
 * @property cacheBytes Combined size of the application's cache directory in bytes.
 * @property isLoading Indicates whether storage usage is currently being computed.
 * @property isClearingCache Indicates whether a cache cleanup operation is currently in progress.
 */
@Immutable
data class StorageUiState(
    val databaseBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val isLoading: Boolean = false,
    val isClearingCache: Boolean = false,
) {
    /**
     * Combined database and cache footprint in bytes.
     */
    val totalBytes: Long
        get() = databaseBytes + cacheBytes

    /**
     * Human-readable representation of [totalBytes].
     */
    val formattedTotalSize: String
        get() = formatStorageBytes(totalBytes)

    /**
     * Human-readable representation of [databaseBytes].
     */
    val formattedDatabaseSize: String
        get() = formatStorageBytes(databaseBytes)

    /**
     * Human-readable representation of [cacheBytes].
     */
    val formattedCacheSize: String
        get() = formatStorageBytes(cacheBytes)
}

/**
 * One-off UI events emitted by [StorageSettingsViewModel].
 */
sealed interface StorageSettingsEvent {
    data object CacheCleared : StorageSettingsEvent
}

private const val BYTES_PER_UNIT = 1024L

/**
 * Formats a byte count into a human-readable string using binary units (`B`, `KB`, `MB`, `GB`).
 *
 * Negative inputs are clamped to `0 B`.
 *
 * @param bytes Size in bytes.
 * @return Formatted size string using [Locale.ROOT] decimal formatting.
 */
internal fun formatStorageBytes(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    if (safeBytes < BYTES_PER_UNIT) return "$safeBytes B"
    val kb = safeBytes / BYTES_PER_UNIT.toDouble()
    if (kb < BYTES_PER_UNIT) return String.format(Locale.ROOT, "%.1f KB", kb)
    val mb = kb / BYTES_PER_UNIT.toDouble()
    if (mb < BYTES_PER_UNIT) return String.format(Locale.ROOT, "%.1f MB", mb)
    val gb = mb / BYTES_PER_UNIT.toDouble()
    return String.format(Locale.ROOT, "%.1f GB", gb)
}
