package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import android.util.Log
import coil3.imageLoader
import com.heckmannch.birthdaybuddy.BirthdayBuddyApplication
import com.heckmannch.birthdaybuddy.data.local.APP_DATABASE_NAME
import com.heckmannch.birthdaybuddy.data.local.SETTINGS_DATABASE_NAME
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.repository.StorageRepository
import com.heckmannch.birthdaybuddy.domain.repository.StorageUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Android implementation of [StorageRepository].
 *
 * Measures both Room databases (`settings_database` and `birthday_database`) including their
 * SQLite sidecar files (`-wal`, `-shm`, `-journal`), calculates the recursive cache directory size,
 * and clears Coil's caches safely before deleting remaining non-Coil cache entries.
 *
 * All I/O operations execute on the injected [@IoDispatcher].
 */
class StorageRepositoryImpl internal constructor(
    private val cacheDirProvider: () -> File,
    private val databasePathProvider: (String) -> File,
    private val clearCoilCaches: () -> Unit,
    private val ioDispatcher: CoroutineDispatcher,
    private val coilCacheDirName: String = BirthdayBuddyApplication.COIL_IMAGE_CACHE_DIR,
    private val logWarning: (String, Throwable?) -> Unit = { message, error ->
        Log.w(TAG, message, error)
    },
) : StorageRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        cacheDirProvider = { context.cacheDir },
        databasePathProvider = { dbName -> context.getDatabasePath(dbName) },
        clearCoilCaches = {
            val loader = context.imageLoader
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        },
        ioDispatcher = ioDispatcher,
    )

    override suspend fun getStorageUsage(): StorageUsage = withContext(ioDispatcher) {
        val databaseBytes = calculateDatabasesSize()
        val cacheBytes = try {
            calculateDirectorySize(cacheDirProvider())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logWarning("Failed to calculate cache directory size.", e)
            0L
        }
        StorageUsage(
            databaseBytes = databaseBytes,
            cacheBytes = cacheBytes,
        )
    }

    override suspend fun clearCache(): Unit = withContext(ioDispatcher) {
        try {
            clearCoilCaches()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logWarning("Failed to clear Coil memory/disk caches.", e)
        }

        // Delete remaining temporary cache entries outside Coil's active disk cache directory
        // so Coil's open DiskLruCache journal is never corrupted.
        try {
            val cacheDir = cacheDirProvider()
            cacheDir.listFiles()?.forEach { child ->
                if (child.name != coilCacheDirName) {
                    child.deleteRecursively()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logWarning("Failed to delete remaining cache files.", e)
        }
    }

    private fun calculateDatabasesSize(): Long {
        return DATABASE_NAMES.sumOf { dbName ->
            try {
                val baseFile = databasePathProvider(dbName)
                SQLITE_FILE_SUFFIXES.sumOf { suffix ->
                    val file = if (suffix.isEmpty()) baseFile else File("${baseFile.path}$suffix")
                    if (file.isFile) file.length() else 0L
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logWarning("Failed to calculate database size for $dbName.", e)
                0L
            }
        }
    }

    private fun calculateDirectorySize(file: File): Long {
        if (!file.exists()) return 0L
        if (!file.isDirectory) return file.length()
        return file.listFiles()?.sumOf { child -> calculateDirectorySize(child) } ?: 0L
    }

    private companion object {
        private const val TAG = "StorageRepositoryImpl"
        private val DATABASE_NAMES = listOf(SETTINGS_DATABASE_NAME, APP_DATABASE_NAME)
        private val SQLITE_FILE_SUFFIXES = listOf("", "-wal", "-shm", "-journal")
    }
}
