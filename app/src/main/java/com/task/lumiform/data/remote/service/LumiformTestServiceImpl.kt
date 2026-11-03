package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.AnswerSubmissionDto
import com.task.lumiform.data.remote.dto.PageDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class LumiformTestServiceImpl @Inject constructor(
    private val api: LumiformApi,
    private val json: Json,
) : LumiformTestService {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getTestCase(): NetworkResult<List<PageDto>> {
        return try {
            /* delay(MOCK_NETWORK_DELAY_MS)
            val response = mockNetworkResponseProvider.getApiResponse<List<PageDto>>(ResponseType.LUMIFORM_TEST_CASE)
            NetworkResult.Success(data = response, code = 200)*/
            NetworkResult.Success(data = api.getTestCase(), code = 200)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            NetworkResult.ApiError(message = e.message(), code = e.code())
        } catch (e: Exception) {
            NetworkResult.NetworkError(e)
        }
    }

    /**
     * There is no submit endpoint yet, so this prints the request body instead of sending it.
     */
    override suspend fun submitAnswers(submission: AnswerSubmissionDto): NetworkResult<Unit> {
        println("[AnswerSync] POST $SUBMIT_ANSWERS_PATH ${json.encodeToString(submission)}")
        return NetworkResult.Success(data = Unit, code = 200)
    }

    private companion object {
        const val SUBMIT_ANSWERS_PATH = "/answers"
        const val MOCK_NETWORK_DELAY_MS = 2_000L
    }
}
