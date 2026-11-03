package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.LumiformItemDto

interface LumiformTestService {
    suspend fun getTestCase(): NetworkResult<List<LumiformItemDto>>
}
