package com.task.lumiform.ui.mapper

import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.ImageQuestionDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.ResponseOptionDomainModel
import com.task.lumiform.domain.model.ResponseSetDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.domain.model.TextQuestionDomainModel
import com.task.lumiform.ui.summary.LumiformSummaryAnswerUiModel
import com.task.lumiform.ui.summary.LumiformSummaryPageUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class LumiformSummaryUiMapperTest {

    @Test
    fun `choice questions in nested sections are flattened in order`() {
        val pages = listOf(
            PageDomainModel(
                id = 1,
                title = "Page",
                items = listOf(
                    choice(id = 2, content = "First"),
                    SectionDomainModel(
                        id = 3,
                        title = "Section",
                        items = listOf(
                            TextQuestionDomainModel(id = 4, content = "Text"),
                            SectionDomainModel(id = 5, title = "Nested", items = listOf(choice(id = 6, content = "Second")))
                        )
                    )
                )
            )
        )

        val result = pages.toSummaryPages(mapOf(2 to listOf(11), 6 to listOf(11, 12)))

        assertEquals(
            listOf(
                LumiformSummaryPageUiModel(
                    id = 1,
                    title = "Page",
                    answers = listOf(
                        LumiformSummaryAnswerUiModel(questionId = 2, question = "First", answers = listOf("A")),
                        LumiformSummaryAnswerUiModel(questionId = 6, question = "Second", answers = listOf("A", "B"))
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `a skipped question has no answers`() {
        val pages = listOf(PageDomainModel(id = 1, title = "Page", items = listOf(choice(id = 2, content = "Skipped"))))

        val result = pages.toSummaryPages(emptyMap())

        assertEquals(emptyList<String>(), result.single().answers.single().answers)
    }

    @Test
    fun `labels follow the response order, not the tap order`() {
        val pages = listOf(PageDomainModel(id = 1, title = "Page", items = listOf(choice(id = 2, content = "Q"))))

        val result = pages.toSummaryPages(mapOf(2 to listOf(12, 11)))

        assertEquals(listOf("A", "B"), result.single().answers.single().answers)
    }

    @Test
    fun `pages without choice questions are left out`() {
        val pages = listOf(
            PageDomainModel(id = 1, title = "Text only", items = listOf(TextQuestionDomainModel(id = 2, content = "Hi"))),
            PageDomainModel(id = 3, title = "Image only", items = listOf(ImageQuestionDomainModel(id = 4, src = "url"))),
            PageDomainModel(id = 5, title = "Empty"),
            PageDomainModel(id = 6, title = "Choice", items = listOf(choice(id = 7, content = "Q")))
        )

        val result = pages.toSummaryPages(emptyMap())

        assertEquals(listOf(6), result.map { it.id })
    }

    private fun choice(id: Int, content: String) = ChoiceQuestionDomainModel(
        id = id,
        content = content,
        responseSet = ResponseSetDomainModel(
            id = id * 10,
            multipleSelection = true,
            responses = listOf(
                ResponseOptionDomainModel(id = 11, label = "A"),
                ResponseOptionDomainModel(id = 12, label = "B")
            )
        )
    )
}
