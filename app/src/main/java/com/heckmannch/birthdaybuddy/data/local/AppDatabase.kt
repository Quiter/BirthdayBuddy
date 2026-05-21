package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Contact::class, LabelConfig::class, NotificationRule::class, PendingNotification::class, AppSettings::class],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 2, to = 3, spec = AppDatabase.DeleteSwipeHintMigration::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    @DeleteColumn(tableName = "app_settings", columnName = "swipeHintShown")
    class DeleteSwipeHintMigration : AutoMigrationSpec

    abstract fun contactDao(): ContactDao
    abstract fun labelConfigDao(): LabelConfigDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun pendingNotificationDao(): PendingNotificationDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Manueller Migrations-Pfad von 1 auf 2.
         * Hintergrund: Bei einigen Usern fehlte die Tabelle 'pending_notifications' in V1,
         * was die Auto-Migration beim Hinzufügen der 'dismissCount' Spalte zum Absturz brachte.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor =
                    db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='pending_notifications'")
                val tableExists = cursor.count > 0
                cursor.close()

                if (!tableExists) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `pending_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactLookupKeys` TEXT NOT NULL, `daysBefore` INTEGER NOT NULL, `year` INTEGER NOT NULL, `isDone` INTEGER NOT NULL, `dismissCount` INTEGER NOT NULL DEFAULT 0)")
                } else {
                    // Spalte nur hinzufügen, wenn sie noch nicht existiert
                    val columnCursor = db.query("PRAGMA table_info(pending_notifications)")
                    var columnExists = false
                    while (columnCursor.moveToNext()) {
                        val nameIndex = columnCursor.getColumnIndex("name")
                        if ((nameIndex != -1) && (columnCursor.getString(nameIndex) == "dismissCount")) {
                            columnExists = true
                            break
                        }
                    }
                    columnCursor.close()
                    if (!columnExists) {
                        db.execSQL("ALTER TABLE pending_notifications ADD COLUMN dismissCount INTEGER NOT NULL DEFAULT 0")
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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
