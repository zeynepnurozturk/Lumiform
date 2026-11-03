package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.AnswerSubmissionDto
import com.task.lumiform.data.remote.dto.PageDto

interface LumiformTestService {
    suspend fun getTestCase(): NetworkResult<List<PageDto>>
    suspend fun submitAnswers(submission: AnswerSubmissionDto): NetworkResult<Unit>
}
