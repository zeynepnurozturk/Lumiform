package com.task.lumiform.data.repo

import com.task.lumiform.data.local.PageDao
import com.task.lumiform.data.local.PageEntity
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
    fun `network error maps to use case error`() = runTest {
        val repository = newRepository(FakeService(NetworkResult.NetworkError(Exception("boom"))))

        val result = repository.getTestCase()

        assertEquals(UseCaseError.NetworkError("boom"), (result as UseCaseResult.Error).error)
    }

    @Test
    fun `saved answers are returned per question and mark the page unsynced`() = runTest {
        val pageDao = FakePageDao()
        val repository = newRepository(FakeService(NetworkResult.Success(listOf(choicePage()))), pageDao)
        repository.getTestCase()

        repository.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))

        assertEquals(mapOf(3 to listOf(5)), repository.getAnswers())
        assertFalse(pageDao.pages.getValue(1).isSynced)
    }

    @Test
    fun `selections survive a refresh and a failed refresh`() = runTest {
        val pageDao = FakePageDao()
        val online = newRepository(FakeService(NetworkResult.Success(listOf(choicePage()))), pageDao)
        online.getTestCase()
        online.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))

        val refreshed = newRepository(FakeService(NetworkResult.Success(listOf(choicePage()))), pageDao)
        val refreshedPage = (refreshed.getTestCase() as UseCaseResult.Success).result.model.single()
        val offline = newRepository(FakeService(NetworkResult.NetworkError(Exception("offline"))), pageDao)
        val offlinePage = (offline.getTestCase() as UseCaseResult.Success).result.model.single()

        listOf(refreshedPage, offlinePage).forEach { page ->
            val section = page.items.first() as SectionDomainModel
            val responses = (section.items.single() as ChoiceQuestionDomainModel).responseSet.responses
            assertEquals(listOf(true, false), responses.map { it.selected })
        }
        assertFalse(pageDao.pages.getValue(1).isSynced)
    }

    @Test
    fun `sync sends the whole page with its selections and marks the page synced`() = runTest {
        val pageDao = FakePageDao()
        val service = FakeService(NetworkResult.Success(listOf(choicePage())))
        val repository = newRepository(service, pageDao)
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
        assertTrue(pageDao.pages.getValue(1).isSynced)
    }

    @Test
    fun `failed sync keeps the page unsynced`() = runTest {
        val pageDao = FakePageDao()
        val service = FakeService(
            NetworkResult.Success(listOf(choicePage())),
            submitResult = NetworkResult.NetworkError(Exception("offline")),
        )
        val repository = newRepository(service, pageDao)
        repository.getTestCase()
        repository.saveAnswer(pageId = 1, questionId = 3, selectedResponseIds = listOf(5))

        val result = repository.syncPageAnswers(pageId = 1)

        assertTrue(result.isFailure())
        assertFalse(pageDao.pages.getValue(1).isSynced)
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
    fun `successful response is cached and served when the next request fails`() = runTest {
        val pageDao = FakePageDao()
        val page = PageDto(id = 1, title = "Cached", items = listOf(SectionDto(id = 2, title = "Section")))
        newRepository(FakeService(NetworkResult.Success(listOf(page))), pageDao = pageDao).getTestCase()

        val offline = newRepository(FakeService(NetworkResult.NetworkError(Exception("offline"))), pageDao = pageDao)
        val result = offline.getTestCase()

        val cached = (result as UseCaseResult.Success).result.model.single()
        assertEquals("Cached", cached.title)
        assertEquals("Section", (cached.items.single() as SectionDomainModel).title)
    }

    @Test
    fun `new response replaces the cached pages`() = runTest {
        val pageDao = FakePageDao()
        newRepository(FakeService(NetworkResult.Success(listOf(PageDto(1), PageDto(2)))), pageDao = pageDao)
            .getTestCase()
        newRepository(FakeService(NetworkResult.Success(listOf(PageDto(3)))), pageDao = pageDao).getTestCase()

        val offline = newRepository(FakeService(NetworkResult.NetworkError(Exception("offline"))), pageDao = pageDao)

        val pages = (offline.getTestCase() as UseCaseResult.Success).result.model
        assertEquals(listOf(3), pages.map { it.id })
    }

    @Test
    fun `failed request with an empty cache returns the error`() = runTest {
        val repository = newRepository(FakeService(NetworkResult.NetworkError(Exception("boom"))))

        val result = repository.getTestCase()

        assertEquals(UseCaseError.NetworkError("boom"), (result as UseCaseResult.Error).error)
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

    private fun newRepository(service: FakeService, pageDao: FakePageDao = FakePageDao()) =
        LumiformTestRepositoryImpl(service, pageDao, Json) { error("WorkManager is not used in these tests") }

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

    private class FakePageDao : PageDao() {
        val pages = linkedMapOf<Int, PageEntity>()
        override suspend fun getAll() = pages.values.sortedBy { it.position }
        override suspend fun getById(pageId: Int) = pages[pageId]
        override suspend fun upsert(page: PageEntity) {
            pages[page.id] = page
        }
        override suspend fun markSynced(pageId: Int, updatedAt: Long) {
            pages[pageId]?.takeIf { it.updatedAt == updatedAt }?.let { pages[pageId] = it.copy(isSynced = true) }
        }
        override suspend fun deleteAll() = pages.clear()
        override suspend fun insertAll(pages: List<PageEntity>) {
            pages.forEach { this.pages[it.id] = it }
        }
    }
}
