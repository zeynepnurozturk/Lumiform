package com.task.lumiform.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.task.lumiform.data.mapper.toRows
import com.task.lumiform.data.remote.dto.ChoiceQuestionDto
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.dto.ResponseOptionDto
import com.task.lumiform.data.remote.dto.ResponseSetDto
import com.task.lumiform.data.remote.dto.SectionDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestCaseDaoTest {

    private lateinit var database: LumiformDatabase
    private lateinit var dao: TestCaseDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LumiformDatabase::class.java
        ).build()
        dao = database.testCaseDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun replaceAllStoresTheWholeTree() = runBlocking {
        dao.replaceAll(listOf(choicePage()).toRows())

        val rows = dao.getTestCase()
        assertEquals(listOf(1, 2, 3, 4), rows.items.map { it.id })
        assertEquals(listOf(3 to 5, 3 to 6, 4 to 6, 4 to 7), rows.responses.map { it.questionId to it.id }.sortedBy { it.first })
        assertNotNull(dao.getPage(1))
        assertNull(dao.getPage(2))
    }

    @Test
    fun saveSelectionChangesOnlyThatQuestionAndMarksThePageUnsynced() = runBlocking {
        dao.replaceAll(listOf(choicePage()).toRows())

        dao.saveSelection(pageId = 1, questionId = 3, responseIds = listOf(6), updatedAt = 42)

        assertEquals(listOf(3 to 6), dao.getSelectedResponses().map { it.questionId to it.id })
        val page = dao.getPage(1)!!
        assertFalse(page.isSynced)
        assertEquals(42, page.updatedAt)
    }

    @Test
    fun saveSelectionWithNoIdsClearsTheQuestion() = runBlocking {
        dao.replaceAll(listOf(choicePage()).toRows())
        dao.saveSelection(pageId = 1, questionId = 3, responseIds = listOf(5, 6), updatedAt = 1)

        dao.saveSelection(pageId = 1, questionId = 3, responseIds = emptyList(), updatedAt = 2)

        assertTrue(dao.getSelectedResponses().isEmpty())
    }

    @Test
    fun saveSelectionOnAMissingPageChangesNothing() = runBlocking {
        dao.replaceAll(listOf(choicePage()).toRows())

        dao.saveSelection(pageId = 99, questionId = 3, responseIds = listOf(5), updatedAt = 1)

        assertTrue(dao.getSelectedResponses().isEmpty())
    }

    @Test
    fun duplicateItemIdsFailAndKeepThePreviousCache() = runBlocking {
        dao.replaceAll(listOf(choicePage()).toRows())

        val failure = runCatching { dao.replaceAll(listOf(PageDto(id = 9), PageDto(id = 9)).toRows()) }

        assertTrue(failure.exceptionOrNull() is SQLiteConstraintException)
        assertEquals(listOf(1, 2, 3, 4), dao.getTestCase().items.map { it.id })
    }

    @Test
    fun replaceAllKeepsSelectionsAndSyncStateOfRowsStillThere() = runBlocking {
        dao.replaceAll(listOf(choicePage()).toRows())
        dao.saveSelection(pageId = 1, questionId = 3, responseIds = listOf(5), updatedAt = 7)
        dao.saveSelection(pageId = 1, questionId = 4, responseIds = listOf(7), updatedAt = 8)

        // Question 4 lost response 7 on the server.
        dao.replaceAll(listOf(choicePage(question4Responses = intArrayOf(6))).toRows())

        assertEquals(listOf(3 to 5), dao.getSelectedResponses().map { it.questionId to it.id })
        val page = dao.getPage(1)!!
        assertFalse(page.isSynced)
        assertEquals(8, page.updatedAt)
    }

    @Test
    fun replaceAllDropsPagesTheServerNoLongerSends() = runBlocking {
        dao.replaceAll(listOf(choicePage(), PageDto(id = 9)).toRows())

        dao.replaceAll(listOf(PageDto(id = 9)).toRows())

        val rows = dao.getTestCase()
        assertEquals(listOf(9), rows.items.map { it.id })
        assertTrue(rows.responses.isEmpty())
    }

    @Test
    fun deletingAPageCascadesToItsItemsAndResponses() = runBlocking {
        dao.replaceAll(listOf(choicePage()).toRows())

        database.openHelper.writableDatabase.execSQL("DELETE FROM items WHERE id = 1")

        val rows = dao.getTestCase()
        assertTrue(rows.items.isEmpty())
        assertTrue(rows.responses.isEmpty())
    }

    @Test
    fun markSyncedOnlyAppliesToTheSameVersion() = runBlocking {
        dao.replaceAll(listOf(choicePage()).toRows())
        dao.saveSelection(pageId = 1, questionId = 3, responseIds = listOf(5), updatedAt = 1)

        dao.markSynced(pageId = 1, updatedAt = 0)
        assertFalse(dao.getPage(1)!!.isSynced)

        dao.markSynced(pageId = 1, updatedAt = 1)
        assertTrue(dao.getPage(1)!!.isSynced)
    }

    // Page 1: choice question 3 inside section 2, and choice question 4 directly on the page.
    private fun choicePage(question4Responses: IntArray = intArrayOf(6, 7)) = PageDto(
        id = 1,
        items = listOf(
            SectionDto(id = 2, title = "Section", items = listOf(choiceQuestion(3, 5, 6))),
            choiceQuestion(4, *question4Responses),
        )
    )

    private fun choiceQuestion(id: Int, vararg responseIds: Int) = ChoiceQuestionDto(
        id = id,
        content = "Question $id",
        responseSet = ResponseSetDto(
            id = id * 10,
            responses = responseIds.map { ResponseOptionDto(id = it, label = "R$it") }
        )
    )
}
