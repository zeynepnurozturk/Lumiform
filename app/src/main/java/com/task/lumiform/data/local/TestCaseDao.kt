package com.task.lumiform.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * All rows of the cached test case. Items are listed parents first, so they can be inserted in that order.
 */
data class TestCaseRows(
    val items: List<ItemEntity>,
    val responses: List<ResponseEntity>,
)

@Dao
abstract class TestCaseDao {

    @Query("SELECT * FROM items ORDER BY position")
    abstract suspend fun getItems(): List<ItemEntity>

    @Query("SELECT * FROM responses ORDER BY position")
    abstract suspend fun getResponses(): List<ResponseEntity>

    @Query("SELECT * FROM items WHERE type = 'PAGE'")
    abstract suspend fun getPages(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :pageId AND type = 'PAGE'")
    abstract suspend fun getPage(pageId: Int): ItemEntity?

    // Pages with answers that have not been sent yet. Emits again whenever the items table changes.
    @Query("SELECT COUNT(*) FROM items WHERE type = 'PAGE' AND isSynced = 0")
    abstract fun observeUnsyncedPageCount(): Flow<Int>

    @Query("SELECT * FROM responses WHERE selected = 1 ORDER BY position")
    abstract suspend fun getSelectedResponses(): List<ResponseEntity>

    @Insert
    abstract suspend fun insertItems(items: List<ItemEntity>)

    @Insert
    abstract suspend fun insertResponses(responses: List<ResponseEntity>)

    @Query("DELETE FROM items")
    abstract suspend fun deleteItems()

    @Query("DELETE FROM responses")
    abstract suspend fun deleteResponses()

    @Query("UPDATE responses SET selected = (id IN (:responseIds)) WHERE questionId = :questionId")
    abstract suspend fun setSelection(questionId: Int, responseIds: List<Int>)

    @Query("UPDATE items SET isSynced = 0, updatedAt = :updatedAt WHERE id = :pageId")
    abstract suspend fun markChanged(pageId: Int, updatedAt: Long)

    // Matching updatedAt keeps a page changed while it was being sent unsynced, so it is sent again.
    @Query("UPDATE items SET isSynced = 1 WHERE id = :pageId AND updatedAt = :updatedAt")
    abstract suspend fun markSynced(pageId: Int, updatedAt: Long)

    // Both reads in one transaction so a save in between never mixes old items with new responses.
    @Transaction
    open suspend fun getTestCase(): TestCaseRows = TestCaseRows(getItems(), getResponses())

    /**
     * Replaces the cached test case with [rows] from the server. The user's selections and each page's sync
     * state are carried over for the rows that are still there. One transaction, so being killed halfway never
     * leaves the cache empty or half replaced.
     */
    @Transaction
    open suspend fun replaceAll(rows: TestCaseRows) {
        val oldPages = getPages().associateBy { it.id }
        val selected = getSelectedResponses().map { it.questionId to it.id }.toSet()
        deleteResponses()
        deleteItems()
        insertItems(
            rows.items.map { item ->
                oldPages[item.id]?.takeIf { item.type == ItemType.PAGE }
                    ?.let { item.copy(isSynced = it.isSynced, updatedAt = it.updatedAt) } ?: item
            }
        )
        insertResponses(rows.responses.map { it.copy(selected = (it.questionId to it.id) in selected) })
    }

    // Removes the whole cached test case with its answers, so the next load starts fresh from the server.
    @Transaction
    open suspend fun clearAll() {
        deleteResponses()
        deleteItems()
    }

    @Transaction
    open suspend fun saveSelection(pageId: Int, questionId: Int, responseIds: List<Int>, updatedAt: Long) {
        if (getPage(pageId) == null) return
        setSelection(questionId, responseIds)
        markChanged(pageId, updatedAt)
    }
}
