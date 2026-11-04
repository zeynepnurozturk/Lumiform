package com.task.lumiform.ui.mapper

import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.ImageQuestionDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.ResponseOptionDomainModel
import com.task.lumiform.domain.model.ResponseSetDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.domain.model.TextQuestionDomainModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel.Choice.SelectionType
import com.task.lumiform.ui.model.LumiformHomePageUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class LumiformHomeUiMapperTest {

    @Test
    fun `pages keep their nesting and choices get a selection type`() {
        val pages = listOf(
            PageDomainModel(
                id = 1,
                title = "Main Page",
                items = listOf(
                    SectionDomainModel(
                        id = 2,
                        title = "Chapter 1",
                        items = listOf(
                            TextQuestionDomainModel(id = 3, content = "Intro"),
                            SectionDomainModel(
                                id = 4,
                                title = "Subsection",
                                items = listOf(ImageQuestionDomainModel(id = 5, src = "url", title = "Image"))
                            )
                        )
                    ),
                    ChoiceQuestionDomainModel(
                        id = 6,
                        content = "Pick",
                        responseSet = ResponseSetDomainModel(id = 7, multipleSelection = true)
                    ),
                    ChoiceQuestionDomainModel(
                        id = 8,
                        content = "Pick one",
                        responseSet = ResponseSetDomainModel(id = 9, multipleSelection = false)
                    )
                )
            )
        )

        val result = pages.toHomePages()

        assertEquals(
            listOf(
                LumiformHomePageUiModel(
                    id = 1,
                    title = "Main Page",
                    items = listOf(
                        LumiformHomeItemUiModel.Section(
                            id = 2,
                            title = "Chapter 1",
                            items = listOf(
                                LumiformHomeItemUiModel.Text(id = 3, content = "Intro"),
                                LumiformHomeItemUiModel.Section(
                                    id = 4,
                                    title = "Subsection",
                                    items = listOf(LumiformHomeItemUiModel.Image(id = 5, src = "url", title = "Image"))
                                )
                            )
                        ),
                        LumiformHomeItemUiModel.Choice(
                            id = 6,
                            content = "Pick",
                            selectionType = SelectionType.Multiple,
                            responses = emptyList()
                        ),
                        LumiformHomeItemUiModel.Choice(
                            id = 8,
                            content = "Pick one",
                            selectionType = SelectionType.Single,
                            responses = emptyList()
                        )
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `choice responses are kept in their order`() {
        val responses = listOf(
            ResponseOptionDomainModel(id = 1, label = "Yes", score = 1),
            ResponseOptionDomainModel(id = 2, label = "No", score = 0)
        )
        val pages = listOf(
            PageDomainModel(
                id = 1,
                items = listOf(
                    ChoiceQuestionDomainModel(
                        id = 2,
                        content = "Pick",
                        responseSet = ResponseSetDomainModel(id = 3, responses = responses)
                    )
                )
            )
        )

        val choice = pages.toHomePages().single().items.single() as LumiformHomeItemUiModel.Choice

        assertEquals(responses, choice.responses)
    }

    @Test
    fun `pages keep their order and an empty page has no items`() {
        val pages = listOf(PageDomainModel(id = 2, title = "Second"), PageDomainModel(id = 1, title = "First"))

        val result = pages.toHomePages()

        assertEquals(listOf(2, 1), result.map { it.id })
        assertEquals(emptyList<LumiformHomeItemUiModel>(), result.first().items)
    }

    @Test
    fun `no pages map to no pages`() {
        assertEquals(emptyList<LumiformHomePageUiModel>(), emptyList<PageDomainModel>().toHomePages())
    }
}
