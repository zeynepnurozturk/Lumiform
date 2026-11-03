package com.task.lumiform.domain.repository

import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseResult

interface LumiformTestRepository {
    suspend fun getTestCase(): UseCaseResult<LumiformTestUseCase.Result>
}
