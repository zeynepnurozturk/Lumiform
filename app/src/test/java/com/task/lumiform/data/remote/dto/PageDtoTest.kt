package com.task.lumiform.data.remote.dto

import java.io.File
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PageDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private fun decode(rawJson: String) = json.decodeFromString<List<PageDto>>(rawJson)

    @Test
    fun `mock response parses into page tree`() {
        val pages = decode(File("src/main/assets/mock/lumiform_test_case_response.json").readText())

        assertEquals(2, pages.size)
        val chapter1 = pages[0].items[1] as SectionDto
        assertTrue(chapter1.items[1] is SectionDto)
        val chapter2 = pages[1].items.single() as SectionDto
        val singleChoice = chapter2.items[1] as ChoiceQuestionDto
        val multiChoice = chapter2.items[2] as ChoiceQuestionDto
        assertEquals(false, singleChoice.responseSet.multipleSelection)
        assertNull(singleChoice.responseSet.responses.last().score)
        assertTrue(multiChoice.responseSet.multipleSelection)
    }

    @Test
    fun `page nested inside page fails`() {
        val rawJson = """[{ "id": 1, "type": "page", "items": [{ "id": 2, "type": "page", "items": [] }] }]"""

        assertThrows(SerializationException::class.java) { decode(rawJson) }
    }

    @Test
    fun `section without title fails`() {
        val rawJson = """[{ "id": 1, "type": "page", "items": [{ "id": 2, "type": "section", "items": [] }] }]"""

        assertThrows(SerializationException::class.java) { decode(rawJson) }
    }

    @Test
    fun `choice without response_set fails`() {
        val rawJson = """[{ "id": 1, "type": "page", "items": [{ "id": 2, "type": "choice", "content": "Q?" }] }]"""

        assertThrows(SerializationException::class.java) { decode(rawJson) }
    }
}
