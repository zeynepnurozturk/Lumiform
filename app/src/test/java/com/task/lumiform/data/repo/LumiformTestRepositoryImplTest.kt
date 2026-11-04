package com.task.lumiform.data.repo

import com.task.lumiform.data.local.ItemEntity
import com.task.lumiform.data.local.ResponseEntity
import com.task.lumiform.data.local.TestCaseDao
import com.task.lumiform.data.mapper.NETWORK_ERROR_MESSAGE
import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.AnswerSubmissionDto
import com.task.lumiform.data.remote.dto.SubmissionChoiceQuestionDto
import com.task.lumiform.data.remote.dto.SubmissionSectionDto
import com.task.lumiform.data.remote.dto.ChoiceQuestionDto
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.dto.ResponseOptionDto
import com.task.lumiform.data.remote.dto.ResponseSetDto
import com.task.lumiform.data.remote.dto.SectionDto
import com.task.lumiform.data.remote.service.LumiformTestService
import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LumiformTestRepositoryImplTest {

    @Test
    fun `success maps dto tree to domain model`() = runTest {
        val dto = PageDto(
            id = 1,
            title = null,
            items = listOf(
                SectionDto(
                    id = 2,
                    title = "Section",
                    items = listOf(
                        ChoiceQuestionDto(
                            id = 3,
                            content = "Question",
                            responseSet = ResponseSetDto(
                                id = 4,
                                multipleSelection = true,
                                responses = listOf(ResponseOptionDto(id = 5, label = "A", score = null))
                            )
                        )
                    )
                )
            )
        )
        val repository = newRepository(FakeService(NetworkResult.Success(listOf(dto))))

        val result = repository.getTestCase()

        val page = (result as UseCaseResult.Success).result.model.single()
        assertEquals("", page.title)
        val section = page.items.single() as SectionDomainModel
        assertEquals("Section", section.title)
        val choice = section.items.single() as ChoiceQuestionDomainModel
        assertTrue(choice.responseSet.multipleSelection)
        assertEquals("A", choice.responseSet.responses.single().label)
    }

    @Test
    fun `network failure maps to a network error without the raw exception message`() = runTest {
        val repository = newRepository(FakeService(NetworkResult.NetworkError(IOException("Unable to resolve host"))))

        val result = repository.getTestCase()

        assertEquals(UseCaseError.NetworkError(NETWORK_ERROR_MESSAGE), (result as UseCaseResult.Error).error)
    }

    @Test
    fun `saved answers are returned per question and mark the page unsynced`() = runTest {
        val dao = FakeTestCaseDao()
        val repository = newRepository(FakeService(NetworkResult.Success(listOf(choicePage()))), dao)
        repository.getTestCase()

        repository.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))

        assertEquals(mapOf(3 to listOf(5)), repository.getAnswers())
        assertFalse(dao.items.getValue(1).isSynced)
    }

    @Test
    fun `selections survive a refresh and a failed refresh`() = runTest {
        val dao = FakeTestCaseDao()
        val online = newRepository(FakeService(NetworkResult.Success(listOf(choicePage()))), dao)
        online.getTestCase()
        online.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))

        val refreshed = newRepository(FakeService(NetworkResult.Success(listOf(choicePage()))), dao)
        val refreshedPage = (refreshed.getTestCase() as UseCaseResult.Success).result.model.single()
        val offline = newRepository(FakeService(NetworkResult.NetworkError(Exception("offline"))), dao)
        val offlinePage = (offline.getTestCase() as UseCaseResult.Success).result.model.single()

        listOf(refreshedPage, offlinePage).forEach { page ->
            val section = page.items.first() as SectionDomainModel
            val responses = (section.items.single() as ChoiceQuestionDomainModel).responseSet.responses
            assertEquals(listOf(true, false), responses.map { it.selected })
        }
        assertFalse(dao.items.getValue(1).isSynced)
    }

    @Test
    fun `sync sends the whole page with its selections and marks the page synced`() = runTest {
        val dao = FakeTestCaseDao()
        val service = FakeService(NetworkResult.Success(listOf(choicePage())))
        val repository = newRepository(service, dao)
        repository.getTestCase()
        repository.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))
        repository.saveAnswer(pageId = 1, questionId = 4, selectedResponseIds = listOf(6))

        val result = repository.syncPageAnswers(pageId = 1)

        assertTrue(result.isSuccessful())
        val submission = service.submissions.single()
        assertEquals(1, submission.id)
        val section = submission.items.first() as SubmissionSectionDto
        val sectionQuestion = section.items.single() as SubmissionChoiceQuestionDto
        assertEquals(listOf(true, false), sectionQuestion.responseSet.responses.map { it.selected })
        val topQuestion = submission.items.last() as SubmissionChoiceQuestionDto
        assertEquals(6, topQuestion.responseSet.responses.single { it.selected }.id)
        assertTrue(dao.items.getValue(1).isSynced)
    }

    @Test
    fun `failed sync keeps the page unsynced`() = runTest {
        val dao = FakeTestCaseDao()
        val service = FakeService(
            NetworkResult.Success(listOf(choicePage())),
            submitResult = NetworkResult.NetworkError(Exception("offline")),
        )
        val repository = newRepository(service, dao)
        repository.getTestCase()
        repository.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))

        val result = repository.syncPageAnswers(pageId = 1)

        assertTrue(result.isFailure())
        assertFalse(dao.items.getValue(1).isSynced)
    }

    @Test
    fun `sync with nothing unsynced sends nothing`() = runTest {
        val service = FakeService(NetworkResult.Success(listOf(choicePage())))
        val repository = newRepository(service)
        repository.getTestCase()

        val result = repository.syncPageAnswers(pageId = 1)

        assertTrue(result.isSuccessful())
        assertNull(service.submissions.firstOrNull())
    }

    @Test
    fun `submit sends every unsynced page and then clears the cache`() = runTest {
        val dao = FakeTestCaseDao()
        val service = FakeService(NetworkResult.Success(listOf(choicePage(), PageDto(id = 9))))
        val repository = newRepository(service, dao)
        repository.getTestCase()
        repository.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))

        val result = repository.submitAll()

        assertTrue(result.isSuccessful())
        assertEquals(listOf(1), service.submissions.map { it.id })
        assertTrue(dao.items.isEmpty())
        assertTrue(dao.responses.isEmpty())
    }

    @Test
    fun `failed submit keeps the cache and the answers`() = runTest {
        val dao = FakeTestCaseDao()
        val service = FakeService(
            NetworkResult.Success(listOf(choicePage())),
            submitResult = NetworkResult.NetworkError(Exception("offline")),
        )
        val repository = newRepository(service, dao)
        repository.getTestCase()
        repository.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))

        val result = repository.submitAll()

        assertTrue(result.isFailure())
        assertEquals(mapOf(3 to listOf(5)), repository.getAnswers())
        assertFalse(dao.items.getValue(1).isSynced)
    }

    @Test
    fun `after submit the next load starts fresh without the old answers`() = runTest {
        val dao = FakeTestCaseDao()
        val repository = newRepository(FakeService(NetworkResult.Success(listOf(choicePage()))), dao)
        repository.getTestCase()
        repository.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))
        repository.submitAll()

        repository.getTestCase()

        assertEquals(emptyMap<Int, List<Int>>(), repository.getAnswers())
        assertTrue(dao.items.getValue(1).isSynced)
    }

    @Test
    fun `successful response is cached and served when the next request fails`() = runTest {
        val dao = FakeTestCaseDao()
        val page = PageDto(id = 1, title = "Cached", items = listOf(SectionDto(id = 2, title = "Section")))
        newRepository(FakeService(NetworkResult.Success(listOf(page))), dao = dao).getTestCase()

        val offline = newRepository(FakeService(NetworkResult.NetworkError(Exception("offline"))), dao = dao)
        val result = offline.getTestCase()

        val cached = (result as UseCaseResult.Success).result.model.single()
        assertEquals("Cached", cached.title)
        assertEquals("Section", (cached.items.single() as SectionDomainModel).title)
    }

    @Test
    fun `new response replaces the cached pages`() = runTest {
        val dao = FakeTestCaseDao()
        newRepository(FakeService(NetworkResult.Success(listOf(PageDto(1), PageDto(2)))), dao = dao)
            .getTestCase()
        newRepository(FakeService(NetworkResult.Success(listOf(PageDto(3)))), dao = dao).getTestCase()

        val offline = newRepository(FakeService(NetworkResult.NetworkError(Exception("offline"))), dao = dao)

        val pages = (offline.getTestCase() as UseCaseResult.Success).result.model
        assertEquals(listOf(3), pages.map { it.id })
    }

    @Test
    fun `failed request with an empty cache returns the error`() = runTest {
        val repository = newRepository(FakeService(NetworkResult.NetworkError(IOException("boom"))))

        val result = repository.getTestCase()

        assertTrue((result as UseCaseResult.Error).error is UseCaseError.NetworkError)
    }

    // Page 1: choice question 3 inside section 2, and choice question 4 directly on the page.
    private fun choicePage() = PageDto(
        id = 1,
        items = listOf(
            SectionDto(id = 2, title = "Section", items = listOf(choiceQuestion(3, 5, 6))),
            choiceQuestion(4, 6, 7),
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

    private fun newRepository(service: FakeService, dao: FakeTestCaseDao = FakeTestCaseDao()) =
        LumiformTestRepositoryImpl(service, dao) { error("WorkManager is not used in these tests") }

    private class FakeService(
        private val result: NetworkResult<List<PageDto>> = NetworkResult.NullResult(),
        private val submitResult: NetworkResult<Unit> = NetworkResult.Success(Unit),
    ) : LumiformTestService {
        val submissions = mutableListOf<AnswerSubmissionDto>()
        override suspend fun getTestCase() = result
        override suspend fun submitAnswers(submission: AnswerSubmissionDto): NetworkResult<Unit> {
            submissions += submission
            return submitResult
        }
    }

    // Keeps the rows in maps; the transaction methods of TestCaseDao run on top of these.
    private class FakeTestCaseDao : TestCaseDao() {
        val items = linkedMapOf<Int, ItemEntity>()
        val responses = linkedMapOf<Pair<Int, Int>, ResponseEntity>()
        override suspend fun getItems() = items.values.sortedBy { it.position }
        override suspend fun getResponses() = responses.values.sortedBy { it.position }
        override suspend fun getPages() = items.values.filter { it.parentId == null }
        override suspend fun getPage(pageId: Int) = items[pageId]?.takeIf { it.parentId == null }
        override suspend fun getSelectedResponses() = getResponses().filter { it.selected }
        override fun observeUnsyncedPageCount() = flowOf(items.values.count { it.parentId == null && !it.isSynced })
        override suspend fun insertItems(items: List<ItemEntity>) {
            items.forEach { this.items[it.id] = it }
        }
        override suspend fun insertResponses(responses: List<ResponseEntity>) {
            responses.forEach { this.responses[it.questionId to it.id] = it }
        }
        override suspend fun deleteItems() = items.clear()
        override suspend fun deleteResponses() = responses.clear()
        override suspend fun setSelection(questionId: Int, responseIds: List<Int>) {
            responses.replaceAll { _, response ->
                if (response.questionId == questionId) response.copy(selected = response.id in responseIds) else response
            }
        }
        override suspend fun markChanged(pageId: Int, updatedAt: Long) {
            items[pageId]?.let { items[pageId] = it.copy(isSynced = false, updatedAt = updatedAt) }
        }
        override suspend fun markSynced(pageId: Int, updatedAt: Long) {
            items[pageId]?.takeIf { it.updatedAt == updatedAt }?.let { items[pageId] = it.copy(isSynced = true) }
        }
    }
}
