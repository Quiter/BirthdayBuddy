package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Testet die Datenbank-Migrationen.
 * WICHTIG: Erfordert die exportierten Schemas in app/schemas.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // 1. Datenbank in Version 1 erstellen
        helper.createDatabase(TEST_DB, 1).apply {
            // Testdaten einfügen (V1-Schema)
            execSQL("INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels) " +
                    "VALUES ('1', 'key1', 'Max Mustermann', '1990-01-01', '[]')")
            close()
        }

        // 2. Migration auf Version 2 ausführen und validieren
        // Wir übergeben die manuelle Migration, die wir testen wollen
        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Erstellt die DB in V1 und migriert schrittweise auf die aktuelle Version
        helper.createDatabase(TEST_DB, 1).close()

        // Room validiert automatisch die AutoMigrations (2->3, 3->4, etc.) 
        // und unsere manuelle Migration (1->2).
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(AppDatabase.MIGRATION_1_2)
         .build().apply {
            openHelper.writableDatabase.close()
        }
    }
}
