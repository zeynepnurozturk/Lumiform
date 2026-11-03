package com.task.lumiform.data.remote.service

import com.task.lumiform.data.remote.dto.PageDto
import retrofit2.http.GET

interface LumiformApi {

    @GET(TEST_CASE_PATH)
    suspend fun getTestCase(): List<PageDto>

    companion object {
        const val BASE_URL = "https://gist.githubusercontent.com/"
        private const val TEST_CASE_PATH =
            "aruana-lumiform/383e1291df3e0cc1d49aeb14d45f5b94/raw/lumiform-android-test.json"
    }
}
