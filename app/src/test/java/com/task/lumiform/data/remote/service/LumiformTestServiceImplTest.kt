package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.PageDto
import java.io.IOException
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class LumiformTestServiceImplTest {

    @Test
    fun `succeeds after failed attempts`() = runTest {
        val api = FakeApi(IOException("timeout"), IOException("timeout"))

        val result = LumiformTestServiceImpl(api, Json).getTestCase()

        assertEquals(listOf(PageDto(1)), (result as NetworkResult.Success).data)
        assertEquals(3, api.calls)
    }

    @Test
    fun `gives up after three attempts and waits longer before each retry`() = runTest {
        val api = FakeApi(IOException("offline"), IOException("offline"), IOException("offline"))

        val result = LumiformTestServiceImpl(api, Json).getTestCase()

        assertTrue(result is NetworkResult.NetworkError)
        assertEquals(3, api.calls)
        assertEquals(1_000L + 2_000L, currentTime)
    }

    @Test
    fun `server error is retried`() = runTest {
        val api = FakeApi(httpError(503))

        val result = LumiformTestServiceImpl(api, Json).getTestCase()

        assertTrue(result is NetworkResult.Success)
        assertEquals(2, api.calls)
    }

    @Test
    fun `client error is not retried`() = runTest {
        val api = FakeApi(httpError(404))

        val result = LumiformTestServiceImpl(api, Json).getTestCase()

        assertEquals(404, (result as NetworkResult.ApiError).code)
        assertEquals(1, api.calls)
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    // Throws the given failures in order, then returns one page.
    private class FakeApi(vararg failures: Exception) : LumiformApi {
        private val failures = failures.toMutableList()
        var calls = 0

        override suspend fun getTestCase(): List<PageDto> {
            calls++
            if (failures.isNotEmpty()) throw failures.removeAt(0)
            return listOf(PageDto(1))
        }
    }
}
