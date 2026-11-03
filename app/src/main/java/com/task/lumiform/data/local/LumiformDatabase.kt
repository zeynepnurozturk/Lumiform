package com.task.lumiform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PageEntity::class], version = 3, exportSchema = false)
abstract class LumiformDatabase : RoomDatabase() {
    abstract fun pageDao(): PageDao

    companion object {
        const val DATABASE_NAME = "lumiform.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pages` (" +
                        "`id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `json` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        // Answers now live in the page rows. The old answers table has no page JSON to merge into, so it is dropped.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pages` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `pages` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DROP TABLE IF EXISTS `answers`")
            }
        }
    }
}
