package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.LumiformItemDto
import com.task.lumiform.data.remote.mock.MockNetworkResponseProvider
import com.task.lumiform.data.remote.mock.MockNetworkResponseProvider.ResponseType
import javax.inject.Inject

class LumiformTestServiceImpl @Inject constructor(
    private val mockNetworkResponseProvider: MockNetworkResponseProvider
) : LumiformTestService {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getTestCase(): NetworkResult<List<LumiformItemDto>> {
        // There is no backend yet, so the response is served from assets/mock.
        return try {
            val response = mockNetworkResponseProvider.getApiResponse<List<LumiformItemDto>>(ResponseType.LUMIFORM_TEST_CASE)
            NetworkResult.Success(data = response, code = 200)
        } catch (e: Exception) {
            NetworkResult.NetworkError(e)
        }
    }
}
