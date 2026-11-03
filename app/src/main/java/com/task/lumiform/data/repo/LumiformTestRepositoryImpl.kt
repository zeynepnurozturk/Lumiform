package com.task.lumiform.data.repo

import com.task.lumiform.data.mapper.toDomainModel
import com.task.lumiform.data.mapper.toUseCaseResult
import com.task.lumiform.data.remote.service.LumiformTestService
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseResult
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LumiformTestRepositoryImpl @Inject internal constructor(
    private val service: LumiformTestService
) : LumiformTestRepository {

    // TODO: Offline support with Room, using normalized tables: pages, items (sections and questions with
    //  parent_id, page_id, position, type) and response_options, rebuilt into the page tree on read.
    // TODO: Network first, cache fallback: on success replace the cache and return it; on failure return
    //  the cached data, and return an error only if the cache is empty.
    override suspend fun getTestCase(): UseCaseResult<LumiformTestUseCase.Result> =
        withContext(Dispatchers.IO) {
            service.getTestCase().toUseCaseResult { items ->
                LumiformTestUseCase.Result(items.map { it.toDomainModel() })
            }
        }
}
