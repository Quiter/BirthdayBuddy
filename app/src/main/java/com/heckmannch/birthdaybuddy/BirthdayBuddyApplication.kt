package com.heckmannch.birthdaybuddy

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toPath
import javax.inject.Inject

/**
 * Main application class for BirthdayBuddy.
 *
 * Key Responsibilities & Architecture:
 * - **Hilt DI Root**: Annotated with [@HiltAndroidApp], serving as the root dependency injection
 *   container for Dagger Hilt. It initiates the application-level dependency graph and component hierarchy.
 * - **On-Demand WorkManager Initialization**: Implements [Configuration.Provider] to lazily configure
 *   [androidx.work.WorkManager] with [HiltWorkerFactory], enabling assisted injection (`@HiltWorker`)
 *   for background tasks (such as periodic widget refreshes and notification scheduling).
 * - **Coil 3 Image Loading & Caching**: Implements [SingletonImageLoader.Factory] to construct the global
 *   [ImageLoader] instance, tuned with specific memory (25%) and disk (2%) caching limits to ensure
 *   jank-free, responsive scrolling across extensive contact lists.
 */
@HiltAndroidApp
class BirthdayBuddyApplication : Application(), Configuration.Provider,
    SingletonImageLoader.Factory {

    /**
     * Injected [HiltWorkerFactory] providing assisted-injection support for Hilt-managed background workers.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Provides custom [Configuration] for WorkManager on demand.
     *
     * This on-demand initialization replaces default startup initialization, avoiding unnecessary
     * startup overhead and ensuring [workerFactory] is properly injected before WorkManager tasks execute.
     *
     * Logging Strategy:
     * Uses compile-time constant [BuildConfig.DEBUG] instead of runtime checks, allowing R8 / ProGuard
     * to perform dead-code elimination for verbose log levels in release builds.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    /**
     * Configures and creates the global [ImageLoader] singleton instance used across the app.
     *
     * Caching & Performance Rationale:
     * - **Memory Cache (25%)**: Allocates up to 25% of available heap memory via [MemoryCache.Builder.maxSizePercent].
     *   This provides ample capacity to keep recently viewed contact avatars in uncompressed memory,
     *   preventing frame drops and ensuring butter-smooth 60/120fps scrolling in contact lists without risking OOM errors.
     * - **Disk Cache (2%)**: Restricts disk storage to 2% of free disk space in the dedicated `"image_cache"` directory.
     *   This bounds the app's persistent storage footprint while retaining cached contact pictures across app restarts
     *   to avoid redundant fetches and decoding from disk or remote sources.
     * - **Crossfade**: Enables seamless visual transitions when loading images into UI components.
     *
     * @param context The platform context provided by Coil.
     * @return Fully configured [ImageLoader] singleton instance.
     */
    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            // Memory caching: 25% heap limit for smooth list scrolling and avatar reuse
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            // Disk caching: 2% disk limit stored under the app's cache directory
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            // Default crossfade transition for polished image loading animations
            .crossfade(true)
            .build()
    }
}
