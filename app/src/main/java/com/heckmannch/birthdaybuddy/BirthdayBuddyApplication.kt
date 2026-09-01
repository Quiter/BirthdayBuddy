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
 * Responsibilities:
 * - Serves as the Dagger Hilt dependency injection root container via [@HiltAndroidApp].
 * - Implements [Configuration.Provider] for on-demand initialization of Android Jetpack [androidx.work.WorkManager]
 *   using [HiltWorkerFactory] to support assisted injection in background workers.
 * - Implements [SingletonImageLoader.Factory] to configure global Coil 3 image loading and caching
 *   behavior (memory and disk cache optimized for contact avatar images).
 */
@HiltAndroidApp
class BirthdayBuddyApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    /**
     * Factory providing assisted-injection support for Hilt-managed background workers.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Provides custom WorkManager configuration on demand.
     *
     * Uses compile-time constant [BuildConfig.DEBUG] instead of runtime bitmask checks,
     * allowing R8 / ProGuard to perform dead-code elimination for log levels in release builds.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    /**
     * Configures and creates the global [ImageLoader] instance used throughout the app.
     *
     * Sets up:
     * - 25% max available memory cache for responsive contact avatar display and fast scrolling.
     * - 2% max disk cache stored in [coil3.PlatformContext.cacheDir]/image_cache.
     * - Default crossfade transition enabled for smooth image rendering.
     *
     * @param context The platform context provided by Coil.
     * @return Configured [ImageLoader] instance.
     */
    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
