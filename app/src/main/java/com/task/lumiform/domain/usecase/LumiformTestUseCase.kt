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

    data class Result(val model: List<PageDomainModel>)
}
