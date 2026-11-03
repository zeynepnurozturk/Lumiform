package com.task.lumiform.data.remote.dto

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LumiformItemDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `mock response parses into item tree`() {
        val rawJson = File("src/main/assets/mock/lumiform_test_case_response.json").readText()

        val pages = json.decodeFromString<List<LumiformItemDto>>(rawJson)

        assertEquals(2, pages.size)
        val secondPage = pages[1] as LumiformItemDto.Page
        val chapter2 = secondPage.items.single() as LumiformItemDto.Section
        val singleChoice = chapter2.items[1] as LumiformItemDto.Choice
        val multiChoice = chapter2.items[2] as LumiformItemDto.Choice
        assertEquals(false, singleChoice.responseSet?.multipleSelection)
        assertNull(singleChoice.responseSet?.responses?.last()?.score)
        assertTrue(multiChoice.responseSet?.multipleSelection == true)

        val subsection = ((pages[0] as LumiformItemDto.Page).items[1] as LumiformItemDto.Section).items[1]
        assertTrue(subsection is LumiformItemDto.Section)
    }
}
