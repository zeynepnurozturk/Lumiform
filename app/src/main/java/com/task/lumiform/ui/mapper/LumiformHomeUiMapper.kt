package com.task.lumiform.ui.mapper

import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.ImageQuestionDomainModel
import com.task.lumiform.domain.model.PageContentDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.domain.model.TextQuestionDomainModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel.Choice.SelectionType
import com.task.lumiform.ui.model.LumiformHomePageUiModel

fun List<PageDomainModel>.toHomePages(): List<LumiformHomePageUiModel> = map { it.toHomePage() }

private fun PageDomainModel.toHomePage() = LumiformHomePageUiModel(
    id = id,
    title = title,
    items = items.map { it.toHomeItem() }
)

private fun PageContentDomainModel.toHomeItem(): LumiformHomeItemUiModel = when (this) {
    is SectionDomainModel -> LumiformHomeItemUiModel.Section(
        id = id,
        title = title,
        items = items.map { it.toHomeItem() }
    )
    is TextQuestionDomainModel -> LumiformHomeItemUiModel.Text(id = id, content = content)
    is ImageQuestionDomainModel -> LumiformHomeItemUiModel.Image(id = id, src = src, title = title)
    is ChoiceQuestionDomainModel -> LumiformHomeItemUiModel.Choice(
        id = id,
        content = content,
        selectionType = responseSet.multipleSelection.toSelectionType(),
        responses = responseSet.responses
    )
}

private fun Boolean.toSelectionType(): SelectionType =
    if (this) SelectionType.Multiple else SelectionType.Single
