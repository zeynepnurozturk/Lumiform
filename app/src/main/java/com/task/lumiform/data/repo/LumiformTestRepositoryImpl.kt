package com.task.lumiform.data.repo

import androidx.work.WorkManager
import com.task.lumiform.data.local.PageDao
import com.task.lumiform.data.local.PageEntity
import com.task.lumiform.data.mapper.toDomainModel
import com.task.lumiform.data.mapper.selectedResponses
import com.task.lumiform.data.mapper.toSubmissionDto
import com.task.lumiform.data.mapper.toUseCaseResult
import com.task.lumiform.data.mapper.withSelections
import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.service.LumiformTestService
import com.task.lumiform.data.sync.AnswerSyncWorker
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseResult
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class LumiformTestRepositoryImpl @Inject internal constructor(
    private val service: LumiformTestService,
    private val pageDao: PageDao,
    private val json: Json,
    // A Provider so WorkManager is only created when something is queued.
    private val workManager: Provider<WorkManager>,
) : LumiformTestRepository {

    /**
     * Network first: a successful response replaces the cached pages, keeping the selections already made.
     * When the request fails the cached pages are returned instead, and the error only surfaces if nothing
     * was cached yet.
     */
    override suspend fun getTestCase(): UseCaseResult<LumiformTestUseCase.Result> =
        withContext(Dispatchers.IO) {
            val response = service.getTestCase()
            val pages = (response as? NetworkResult.Success)?.data
            if (pages != null) {
                val merged = cachePages(pages)
                return@withContext UseCaseResult.Success(LumiformTestUseCase.Result(merged.map { it.toDomainModel() }))
            }
            val cached = pageDao.getAll().mapNotNull { it.decode() }
            if (cached.isEmpty()) {
                response.toUseCaseResult { LumiformTestUseCase.Result(emptyList()) }
            } else {
                println("[PageDb] request failed, showing ${cached.size} cached pages")
                UseCaseResult.Success(LumiformTestUseCase.Result(cached.map { it.toDomainModel() }))
            }
        }

    // Carries selections and sync state over from the pages already cached. Pages the server dropped are gone.
    private suspend fun cachePages(pages: List<PageDto>): List<PageDto> {
        val existing = pageDao.getAll().associateBy { it.id }
        val merged = pages.map { page ->
            existing[page.id]?.decode()?.let { page.withSelections(it.selectedResponses()) } ?: page
        }
        pageDao.replaceAll(
            merged.mapIndexed { index, page ->
                val old = existing[page.id]
                PageEntity(
                    id = page.id,
                    position = index,
                    json = json.encodeToString(page),
                    isSynced = old?.isSynced ?: true,
                    updatedAt = old?.updatedAt ?: 0,
                )
            }
        )
        return merged
    }

    // A row that no longer decodes (for example after a DTO change) is skipped instead of failing the screen.
    private fun PageEntity.decode(): PageDto? =
        runCatching { this@LumiformTestRepositoryImpl.json.decodeFromString<PageDto>(json) }.getOrNull()

    override suspend fun getAnswers(): Map<Int, List<Int>> =
        pageDao.getAll().mapNotNull { it.decode() }
            .flatMap { it.selectedResponses().entries }
            .filter { it.value.isNotEmpty() }
            .associate { it.key to it.value }

    override suspend fun saveAnswer(pageId: Int, questionId: Int, selectedResponseIds: List<Int>) {
        val entity = pageDao.getById(pageId) ?: return
        val page = entity.decode() ?: return
        val updated = entity.copy(
            json = json.encodeToString(page.withSelections(mapOf(questionId to selectedResponseIds))),
            isSynced = false,
            updatedAt = System.currentTimeMillis(),
        )
        pageDao.upsert(updated)
        println("[PageDb] saved question $questionId on page $pageId: $selectedResponseIds")
    }

    override fun queuePageForSync(pageId: Int) {
        AnswerSyncWorker.enqueue(workManager.get(), pageId)
    }

    override suspend fun syncPageAnswers(pageId: Int): UseCaseResult<Unit> {
        val entity = pageDao.getById(pageId)
        val page = entity?.takeIf { !it.isSynced }?.decode()
        if (entity == null || page == null) {
            println("[AnswerSync] page $pageId: nothing to send")
            return UseCaseResult.Success(Unit)
        }
        val result = service.submitAnswers(page.toSubmissionDto())
        if (result is NetworkResult.Success) {
            pageDao.markSynced(pageId, entity.updatedAt)
            println("[PageDb] marked synced: page $pageId")
        }
        return result.toUseCaseResult(emptyObject = Unit) { }
    }
}
