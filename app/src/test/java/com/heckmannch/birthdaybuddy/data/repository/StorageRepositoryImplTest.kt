package com.heckmannch.birthdaybuddy.data.repository

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.BirthdayBuddyApplication
import com.heckmannch.birthdaybuddy.data.local.APP_DATABASE_NAME
import com.heckmannch.birthdaybuddy.data.local.SETTINGS_DATABASE_NAME
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `getStorageUsage sums both databases including wal, shm, and journal files`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dbDir = tempFolder.newFolder("databases")
        val cacheDir = tempFolder.newFolder("cache")

        writeBytes(File(dbDir, SETTINGS_DATABASE_NAME), 100)
        writeBytes(File(dbDir, "$SETTINGS_DATABASE_NAME-wal"), 50)
        writeBytes(File(dbDir, "$SETTINGS_DATABASE_NAME-shm"), 25)
        writeBytes(File(dbDir, "$SETTINGS_DATABASE_NAME-journal"), 15)

        writeBytes(File(dbDir, APP_DATABASE_NAME), 300)
        writeBytes(File(dbDir, "$APP_DATABASE_NAME-wal"), 120)
        writeBytes(File(dbDir, "$APP_DATABASE_NAME-shm"), 30)
        // Intentionally omit birthday_database-journal to verify missing sidecars are skipped safely

        // Unrelated file in database directory should not be counted
        writeBytes(File(dbDir, "other_database"), 999)

        val repository = StorageRepositoryImpl(
            cacheDirProvider = { cacheDir },
            databasePathProvider = { dbName -> File(dbDir, dbName) },
            clearCoilCaches = {},
            ioDispatcher = testDispatcher,
            logWarning = { _, _ -> },
        )

        val usage = repository.getStorageUsage()

        assertThat(usage.databaseBytes).isEqualTo(100L + 50L + 25L + 15L + 300L + 120L + 30L)
        assertThat(usage.cacheBytes).isEqualTo(0L)
        assertThat(usage.totalBytes).isEqualTo(640L)
    }

    @Test
    fun `getStorageUsage recursively sums files in cache directory`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dbDir = tempFolder.newFolder("databases")
        val cacheDir = tempFolder.newFolder("cache")
        val imageCacheDir = File(cacheDir, BirthdayBuddyApplication.COIL_IMAGE_CACHE_DIR).apply { mkdirs() }
        val nestedDir = File(cacheDir, "other/nested").apply { mkdirs() }

        writeBytes(File(cacheDir, "root.tmp"), 40)
        writeBytes(File(imageCacheDir, "avatar_1"), 200)
        writeBytes(File(nestedDir, "export.tmp"), 60)

        val repository = StorageRepositoryImpl(
            cacheDirProvider = { cacheDir },
            databasePathProvider = { dbName -> File(dbDir, dbName) },
            clearCoilCaches = {},
            ioDispatcher = testDispatcher,
            logWarning = { _, _ -> },
        )

        val usage = repository.getStorageUsage()

        assertThat(usage.databaseBytes).isEqualTo(0L)
        assertThat(usage.cacheBytes).isEqualTo(300L)
        assertThat(usage.totalBytes).isEqualTo(300L)
    }

    @Test
    fun `clearCache clears Coil caches first and deletes only non-Coil cache entries`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dbDir = tempFolder.newFolder("databases")
        val cacheDir = tempFolder.newFolder("cache")
        val imageCacheDir = File(cacheDir, BirthdayBuddyApplication.COIL_IMAGE_CACHE_DIR).apply { mkdirs() }
        val coilJournal = writeBytes(File(imageCacheDir, "journal"), 16)
        val coilEntry = writeBytes(File(imageCacheDir, "entry_0"), 128)

        val tempFile = writeBytes(File(cacheDir, "temp.dat"), 64)
        val otherDir = File(cacheDir, "misc_cache").apply { mkdirs() }
        val otherNestedFile = writeBytes(File(otherDir, "nested.dat"), 32)

        val executionLog = mutableListOf<String>()
        val repository = StorageRepositoryImpl(
            cacheDirProvider = { cacheDir },
            databasePathProvider = { dbName -> File(dbDir, dbName) },
            clearCoilCaches = {
                executionLog.add("coil_cleared")
                // Verify non-Coil cache files still exist when Coil clear runs first
                assertThat(tempFile.exists()).isTrue()
                assertThat(otherNestedFile.exists()).isTrue()
                // Simulate Coil's diskCache.clear() removing cached entries while preserving its journal
                coilEntry.delete()
            },
            ioDispatcher = testDispatcher,
            logWarning = { _, _ -> },
        )

        repository.clearCache()

        assertThat(executionLog).containsExactly("coil_cleared")
        // Non-Coil cache entries were removed
        assertThat(tempFile.exists()).isFalse()
        assertThat(otherDir.exists()).isFalse()
        // Coil's directory and journal remain intact (not recursively deleted)
        assertThat(imageCacheDir.exists()).isTrue()
        assertThat(coilJournal.exists()).isTrue()
        assertThat(coilEntry.exists()).isFalse()
    }

    @Test
    fun `clearCache still deletes non-Coil files when Coil cache clear throws`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dbDir = tempFolder.newFolder("databases")
        val cacheDir = tempFolder.newFolder("cache")
        val tempFile = writeBytes(File(cacheDir, "temp.dat"), 64)
        val warnings = mutableListOf<String>()

        val repository = StorageRepositoryImpl(
            cacheDirProvider = { cacheDir },
            databasePathProvider = { dbName -> File(dbDir, dbName) },
            clearCoilCaches = { throw IllegalStateException("Coil failure") },
            ioDispatcher = testDispatcher,
            logWarning = { msg, _ -> warnings.add(msg) },
        )

        repository.clearCache()

        assertThat(tempFile.exists()).isFalse()
        assertThat(warnings).hasSize(1)
    }

    private fun writeBytes(file: File, size: Int): File {
        file.writeBytes(ByteArray(size) { 1 })
        return file
    }
}
