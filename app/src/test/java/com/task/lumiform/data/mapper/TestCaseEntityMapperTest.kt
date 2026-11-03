package com.task.lumiform.data.mapper

import com.task.lumiform.data.local.ItemEntity
import com.task.lumiform.data.local.ItemType
import com.task.lumiform.data.local.TestCaseRows
import com.task.lumiform.data.remote.dto.ChoiceQuestionDto
import com.task.lumiform.data.remote.dto.ImageQuestionDto
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.dto.ResponseOptionDto
import com.task.lumiform.data.remote.dto.ResponseSetDto
import com.task.lumiform.data.remote.dto.SectionDto
import com.task.lumiform.data.remote.dto.TextQuestionDto
import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.ImageQuestionDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.domain.model.TextQuestionDomainModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestCaseEntityMapperTest {

    // Page 1: section 2 holding text 3 and section 4 (image 5), then choice 6. Page 7: choice 8.
    // Choices 6 and 8 both have a response with id 100.
    private val pages = listOf(
        PageDto(
            id = 1,
            title = "Page",
            items = listOf(
                SectionDto(
                    id = 2,
                    title = "Section",
                    items = listOf(
                        TextQuestionDto(id = 3, content = "Text"),
                        SectionDto(id = 4, title = "Nested", items = listOf(ImageQuestionDto(5, "src", "Image"))),
                    )
                ),
                choice(id = 6, multipleSelection = true, 100, 101),
            )
        ),
        PageDto(id = 7, items = listOf(choice(id = 8, multipleSelection = false, 100))),
    )

    @Test
    fun `rows rebuild the same tree`() {
        val rebuilt = pages.toRows().toDomainModel()

        assertEquals(listOf(1, 7), rebuilt.map { it.id })
        val first = rebuilt[0]
        assertEquals("Page", first.title)
        val section = first.items[0] as SectionDomainModel
        assertEquals("Text", (section.items[0] as TextQuestionDomainModel).content)
        val nested = section.items[1] as SectionDomainModel
        assertEquals("Nested", nested.title)
        assertEquals(ImageQuestionDomainModel(5, "src", "Image"), nested.items.single())
        val choice = first.items[1] as ChoiceQuestionDomainModel
        assertEquals(60, choice.responseSet.id)
        assertTrue(choice.responseSet.multipleSelection)
        assertEquals(listOf("R100", "R101"), choice.responseSet.responses.map { it.label })
        assertEquals("", rebuilt[1].title)
    }

    @Test
    fun `items come after their parent`() {
        val items = pages.toRows().items

        items.forEachIndexed { index, item ->
            val parentIndex = items.indexOfFirst { it.id == item.parentId }
            assertTrue("item ${item.id} before its parent", item.parentId == null || parentIndex in 0 until index)
        }
    }

    @Test
    fun `same response id under two questions stays separate`() {
        val responses = pages.toRows().responses

        assertEquals(listOf(6 to 100, 6 to 101, 8 to 100), responses.map { it.questionId to it.id })
    }

    @Test
    fun `children are ordered by position and stray rows are skipped`() {
        val rows = TestCaseRows(
            items = listOf(
                ItemEntity(id = 1, parentId = null, position = 0, type = ItemType.PAGE),
                ItemEntity(id = 3, parentId = 1, position = 1, type = ItemType.TEXT, content = "second"),
                ItemEntity(id = 2, parentId = 1, position = 0, type = ItemType.TEXT, content = "first"),
                ItemEntity(id = 4, parentId = 1, position = 2, type = ItemType.PAGE),
                ItemEntity(id = 5, parentId = 1, position = 3, type = ItemType.CHOICE, responseSetId = null),
            ),
            responses = emptyList(),
        )

        val page = rows.toDomainModel().single()

        assertEquals(listOf("first", "second"), page.items.map { (it as TextQuestionDomainModel).content })
    }

    private fun choice(id: Int, multipleSelection: Boolean, vararg responseIds: Int) = ChoiceQuestionDto(
        id = id,
        content = "Question $id",
        responseSet = ResponseSetDto(
            id = id * 10,
            multipleSelection = multipleSelection,
            responses = responseIds.map { ResponseOptionDto(id = it, label = "R$it") }
        )
    )
}
