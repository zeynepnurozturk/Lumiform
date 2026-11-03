package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.mock.MockNetworkResponseProvider
import com.task.lumiform.data.remote.mock.MockNetworkResponseProvider.ResponseType
import kotlinx.coroutines.delay
import javax.inject.Inject

class LumiformTestServiceImpl @Inject constructor(
    private val mockNetworkResponseProvider: MockNetworkResponseProvider
) : LumiformTestService {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getTestCase(): NetworkResult<List<PageDto>> {
        return try {
            delay(MOCK_NETWORK_DELAY_MS)
            val response = mockNetworkResponseProvider.getApiResponse<List<PageDto>>(ResponseType.LUMIFORM_TEST_CASE)
            NetworkResult.Success(data = response, code = 200)
        } catch (e: Exception) {
            NetworkResult.NetworkError(e)
        }
    }

    private companion object {
        const val MOCK_NETWORK_DELAY_MS = 2_000L
    }
}
