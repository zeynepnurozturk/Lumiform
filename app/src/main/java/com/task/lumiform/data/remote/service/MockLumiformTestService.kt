package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.AnswerSubmissionDto
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.mock.MockNetworkResponseProvider
import com.task.lumiform.data.remote.mock.MockNetworkResponseProvider.ResponseType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import javax.inject.Inject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * Reads the bundled JSON from `assets/mock/` instead of calling the gist. It was used while the API response
 * wasn't final. To use it, switch the binding in [com.task.lumiform.di.NetworkModule].
 */
class MockLumiformTestService @Inject constructor(
    private val mockNetworkResponseProvider: MockNetworkResponseProvider,
    private val json: Json,
) : LumiformTestService {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getTestCase(): NetworkResult<List<PageDto>> {
        return try {
            delay(2.seconds)
            val response = mockNetworkResponseProvider.getApiResponse<List<PageDto>>(ResponseType.LUMIFORM_TEST_CASE)
            NetworkResult.Success(data = response, code = 200)
        } catch (e: Exception) {
            NetworkResult.NetworkError(e)
        }
    }

    /**
     * There is no submit endpoint yet, so this logs the request body instead of sending it.
     */
    override suspend fun submitAnswers(submission: AnswerSubmissionDto): NetworkResult<Unit> {
        delay(2.seconds)
        // Only debug builds plant a tree, so release builds skip encoding the body.
        if (Timber.treeCount > 0) {
            Timber.tag("AnswerSync").d("POST $SUBMIT_ANSWERS_PATH ${json.encodeToString(submission)}")
        }
        return NetworkResult.Success(data = Unit, code = 200)
    }

    private companion object {
        const val SUBMIT_ANSWERS_PATH = "/answers"
    }
}
