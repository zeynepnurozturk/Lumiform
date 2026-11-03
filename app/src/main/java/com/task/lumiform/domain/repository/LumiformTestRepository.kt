package com.task.lumiform.domain.repository

import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseResult

interface LumiformTestRepository {
    suspend fun getTestCase(): UseCaseResult<LumiformTestUseCase.Result>

    /**
     * Saved selected response ids per choice question id.
     */
    suspend fun getAnswers(): Map<Int, List<Int>>

    suspend fun saveAnswer(pageId: Int, questionId: Int, selectedResponseIds: List<Int>)

    /**
     * Sends the page's unsynced answers in the background as soon as there is a connection.
     */
    fun queuePageForSync(pageId: Int)

    suspend fun syncPageAnswers(pageId: Int): UseCaseResult<Unit>
}
