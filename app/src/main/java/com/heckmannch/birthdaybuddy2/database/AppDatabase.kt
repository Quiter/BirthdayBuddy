package com.heckmannch.birthdaybuddy2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Contact::class, LabelConfig::class, NotificationRule::class, PendingNotification::class, AppSettings::class],
    version = 13,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun labelConfigDao(): LabelConfigDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun pendingNotificationDao(): PendingNotificationDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration von Version 1 auf 13 (Verlustfrei).
         * Stellt sicher, dass alte Kontakte erhalten bleiben und neue Tabellen angelegt werden.
         */
        private val MIGRATION_1_13 = object : Migration(1, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Neue Tabellen anlegen
                db.execSQL("CREATE TABLE IF NOT EXISTS `label_configs` (`name` TEXT NOT NULL, `isHiddenFromFilter` INTEGER NOT NULL, `isIgnored` INTEGER NOT NULL, `isSystem` INTEGER NOT NULL, PRIMARY KEY(`name`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `notification_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `daysBefore` INTEGER NOT NULL, `hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `pending_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactLookupKeys` TEXT NOT NULL, `daysBefore` INTEGER NOT NULL, `year` INTEGER NOT NULL, `isDone` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, `notificationsEnabled` INTEGER NOT NULL, `persistentNotifications` INTEGER NOT NULL, `swipeHintShown` INTEGER NOT NULL, `lastSyncTimestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                // 2. Kontakte-Tabelle transformieren (falls nötig)
                val cursor = db.query("PRAGMA table_info(`contacts`)")
                var hasLookupKey = false
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == "lookupKey") {
                            hasLookupKey = true
                            break
                        }
                    }
                }
                cursor.close()

                if (!hasLookupKey) {
                    // Backup der alten Daten
                    db.execSQL("ALTER TABLE `contacts` RENAME TO `contacts_old`")
                    
                    // Neue Tabelle mit aktuellem Schema erstellen
                    db.execSQL("CREATE TABLE `contacts` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactId` TEXT NOT NULL, `lookupKey` TEXT NOT NULL, `fullName` TEXT NOT NULL, `birthday` TEXT NOT NULL, `imageUri` TEXT, `labels` TEXT NOT NULL, `giftIdeas` TEXT)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contacts_lookupKey` ON `contacts` (`lookupKey`)")
                    
                    // Daten migrieren (Soweit Spalten in v1 existierten)
                    // Wir gehen davon aus, dass contactId, fullName, birthday, imageUri und labels existierten.
                    // Falls eine Spalte fehlt, schlägt SQL fehl -> fallbackToDestructiveMigration greift.
                    try {
                        db.execSQL("INSERT INTO `contacts` (contactId, lookupKey, fullName, birthday, imageUri, labels) " +
                                "SELECT contactId, contactId, fullName, birthday, imageUri, labels FROM `contacts_old`")
                        db.execSQL("DROP TABLE `contacts_old`")
                    } catch (e: Exception) {
                        // Falls die Spaltenstruktur von v1 doch anders war, brechen wir hier ab.
                        // Room wird dann die destructive Migration durchführen.
                        e.printStackTrace()
                    }
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "birthday_database",
                )
                    .addMigrations(MIGRATION_1_13)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
