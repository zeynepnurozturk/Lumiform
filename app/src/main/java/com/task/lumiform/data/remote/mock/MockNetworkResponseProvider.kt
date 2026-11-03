package com.task.lumiform.data.remote.mock

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Reads mock API responses from `assets/mock/`.
 *
 * To add a new mock:
 * 1. Put the JSON file into `app/src/main/assets/mock/`.
 * 2. Add a [ResponseType] entry with its file name.
 * 3. Call `getApiResponse<YourResponse>(ResponseType.YOUR_ENTRY)`.
 */
class MockNetworkResponseProvider(
    val context: Context,
    val json: Json
) {

    enum class ResponseType(val fileName: String) {
        LUMIFORM_TEST_CASE("lumiform_test_case_response.json")
    }

    inline fun <reified T> getApiResponse(responseType: ResponseType): T {
        val rawJson = context.assets.open("mock/${responseType.fileName}").bufferedReader().use { it.readText() }
        return json.decodeFromString(rawJson)
    }
}
