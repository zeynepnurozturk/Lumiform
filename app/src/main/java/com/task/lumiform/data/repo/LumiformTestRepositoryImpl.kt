package com.task.lumiform.data.repo

import android.database.sqlite.SQLiteException
import androidx.work.WorkManager
import com.task.lumiform.data.local.TestCaseDao
import com.task.lumiform.data.mapper.toDomainModel
import com.task.lumiform.data.mapper.toRows
import com.task.lumiform.data.mapper.toSubmissionDto
import com.task.lumiform.data.mapper.toUseCaseResult
import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.service.LumiformTestService
import com.task.lumiform.data.sync.AnswerSyncWorker
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseResult
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LumiformTestRepositoryImpl @Inject internal constructor(
    private val service: LumiformTestService,
    private val testCaseDao: TestCaseDao,
    // A Provider so WorkManager is only created when something is queued.
    private val workManager: Provider<WorkManager>,
) : LumiformTestRepository {

    /**
     * Network first: a successful response replaces the cached test case, keeping the selections already made.
     * The pages are always read back from Room, so they carry those selections. When the request fails the
     * cached pages are returned instead, and the error only surfaces if nothing was cached yet.
     */
    override suspend fun getTestCase(): UseCaseResult<LumiformTestUseCase.Result> =
        withContext(Dispatchers.IO) {
            val response = service.getTestCase()
            val pages = (response as? NetworkResult.Success)?.data
            val refreshed = pages != null && cachePages(pages)

            val cached = loadPages()
            if (!refreshed && cached.isEmpty()) {
                return@withContext response.toUseCaseResult { LumiformTestUseCase.Result(emptyList()) }
            }
            if (!refreshed) println("[PageDb] no fresh pages, showing ${cached.size} cached pages")
            UseCaseResult.Success(LumiformTestUseCase.Result(cached))
        }

    // Item ids are expected to be unique across the whole test case. If the server breaks that, the insert fails,
    // the transaction rolls back and the previous cache is kept.
    private suspend fun cachePages(pages: List<PageDto>): Boolean = try {
        testCaseDao.replaceAll(pages.toRows())
        true
    } catch (e: SQLiteException) {
        println("[PageDb] could not cache the response: ${e.message}")
        false
    }

    private suspend fun loadPages(): List<PageDomainModel> = testCaseDao.getTestCase().toDomainModel()

    override suspend fun getAnswers(): Map<Int, List<Int>> =
        testCaseDao.getSelectedResponses().groupBy(keySelector = { it.questionId }, valueTransform = { it.id })

    override suspend fun saveAnswer(pageId: Int, questionId: Int, selectedResponseIds: List<Int>) {
        testCaseDao.saveSelection(pageId, questionId, selectedResponseIds, updatedAt = System.currentTimeMillis())
        println("[PageDb] saved question $questionId on page $pageId: $selectedResponseIds")
    }

    override fun queuePageForSync(pageId: Int) {
        AnswerSyncWorker.enqueue(workManager.get(), pageId)
    }

    override suspend fun syncPageAnswers(pageId: Int): UseCaseResult<Unit> {
        val pageRow = testCaseDao.getPage(pageId)?.takeIf { !it.isSynced }
        val page = pageRow?.let { loadPages().firstOrNull { it.id == pageId } }
        if (pageRow == null || page == null) {
            println("[AnswerSync] page $pageId: nothing to send")
            return UseCaseResult.Success(Unit)
        }
        val result = service.submitAnswers(page.toSubmissionDto())
        if (result is NetworkResult.Success) {
            testCaseDao.markSynced(pageId, pageRow.updatedAt)
            println("[PageDb] marked synced: page $pageId")
        }
        return result.toUseCaseResult(emptyObject = Unit) { }
    }
}
