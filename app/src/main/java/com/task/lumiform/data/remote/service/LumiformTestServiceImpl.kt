package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.AnswerSubmissionDto
import com.task.lumiform.data.remote.dto.PageDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import timber.log.Timber

class LumiformTestServiceImpl @Inject constructor(
    private val api: LumiformApi,
    private val json: Json,
) : LumiformTestService {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getTestCase(): NetworkResult<List<PageDto>> {
        return try {
            NetworkResult.Success(data = withRetry { api.getTestCase() }, code = 200)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            NetworkResult.ApiError(message = e.message(), code = e.code())
        } catch (e: Exception) {
            NetworkResult.NetworkError(e)
        }
    }

    /**
     * Runs [request] up to [MAX_ATTEMPTS] times. Only a lost connection, a timeout or a 5xx response is retried,
     * since anything else would fail the same way again. The wait before each retry grows by [RETRY_DELAY_MS].
     */
    private suspend fun <T> withRetry(request: suspend () -> T): T {
        repeat(MAX_ATTEMPTS - 1) { attempt ->
            try {
                return request()
            } catch (e: IOException) {
                // Retried below.
            } catch (e: HttpException) {
                if (e.code() < HTTP_SERVER_ERROR) throw e
            }
            delay(RETRY_DELAY_MS * (attempt + 1))
        }
        return request()
    }

    /**
     * There is no submit endpoint yet, so this logs the request body instead of sending it.
     */
    override suspend fun submitAnswers(submission: AnswerSubmissionDto): NetworkResult<Unit> {
        // Only debug builds plant a tree, so release builds skip encoding the body.
        if (Timber.treeCount > 0) {
            Timber.tag("AnswerSync").d("POST $SUBMIT_ANSWERS_PATH ${json.encodeToString(submission)}")
        }
        return NetworkResult.Success(data = Unit, code = 200)
    }

    private companion object {
        const val SUBMIT_ANSWERS_PATH = "/answers"
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1_000L
        const val HTTP_SERVER_ERROR = 500
    }
}
