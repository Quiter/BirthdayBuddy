package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Contact::class, PendingNotification::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun pendingNotificationDao(): PendingNotificationDao

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
                    .fallbackToDestructiveMigration() // Radikaler Schnitt für V7 (Cache-Wipe)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
