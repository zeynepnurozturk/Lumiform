package com.task.lumiform.ui.mapper

import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.ImageQuestionDomainModel
import com.task.lumiform.domain.model.PageDomainModel
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
}
