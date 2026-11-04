package com.task.lumiform.domain.usecase

import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.repository.LumiformTestRepository
import javax.inject.Inject

open class LumiformTestUseCase @Inject constructor(
    private val repository: LumiformTestRepository
) {
    open suspend operator fun invoke(): UseCaseResult<Result> {
        return repository.getTestCase()
    }

    open suspend fun getAnswers(): Map<Int, List<Int>> = repository.getAnswers()

    open suspend fun saveAnswer(pageId: Int, questionId: Int, selectedResponseIds: List<Int>) =
        repository.saveAnswer(pageId, questionId, selectedResponseIds)

    open fun queuePageForSync(pageId: Int) = repository.queuePageForSync(pageId)

    open suspend fun submitAll(): UseCaseResult<Unit> = repository.submitAll()

    data class Result(val pages:List<PageDomainModel>)
}
