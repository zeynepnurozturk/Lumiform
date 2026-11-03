package com.task.lumiform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ItemEntity::class, ResponseEntity::class], version = 1, exportSchema = false)
abstract class LumiformDatabase : RoomDatabase() {
    abstract fun testCaseDao(): TestCaseDao

    companion object {
        const val DATABASE_NAME = "lumiform.db"
    }
}
