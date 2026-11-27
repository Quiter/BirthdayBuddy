package com.heckmannch.birthdaybuddy.domain.repository

/**
 * Snapshot of the application's local storage footprint in bytes.
 *
 * @property databaseBytes Combined size of all Room databases including SQLite sidecar files (`-wal`, `-shm`, `-journal`).
 * @property cacheBytes Combined size of all files within the application's cache directory.
 */
data class StorageUsage(
    val databaseBytes: Long = 0L,
    val cacheBytes: Long = 0L,
) {
    /**
     * Combined database and cache size in bytes.
     */
    val totalBytes: Long
        get() = databaseBytes + cacheBytes
}

/**
 * Repository for inspecting and clearing local application storage (databases and caches).
 *
 * All `suspend` operations are main-safe and offload filesystem and cache I/O to an I/O dispatcher.
 */
interface StorageRepository {

    /**
     * Calculates the current storage footprint for Room databases and application caches.
     *
     * @return The current [StorageUsage] in bytes.
     */
    suspend fun getStorageUsage(): StorageUsage

    /**
     * Clears Coil's memory and disk caches safely via Coil's APIs and removes remaining temporary
     * cache files outside Coil's active disk cache directory.
     */
    suspend fun clearCache()
}
