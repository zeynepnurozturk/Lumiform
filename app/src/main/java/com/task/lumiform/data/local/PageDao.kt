package com.task.lumiform.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
abstract class PageDao {

    @Query("SELECT * FROM pages ORDER BY position")
    abstract suspend fun getAll(): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :pageId")
    abstract suspend fun getById(pageId: Int): PageEntity?

    @Upsert
    abstract suspend fun upsert(page: PageEntity)

    // Matching updatedAt keeps a page changed while it was being sent unsynced, so it is sent again.
    @Query("UPDATE pages SET isSynced = 1 WHERE id = :pageId AND updatedAt = :updatedAt")
    abstract suspend fun markSynced(pageId: Int, updatedAt: Long)

    @Query("DELETE FROM pages")
    abstract suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(pages: List<PageEntity>)

    // One transaction so being killed halfway never leaves the cache empty or half replaced.
    @Transaction
    open suspend fun replaceAll(pages: List<PageEntity>) {
        deleteAll()
        insertAll(pages)
    }
}
