package com.task.lumiform.ui.mapper

import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.PageContentDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.ui.summary.LumiformSummaryAnswerUiModel
import com.task.lumiform.ui.summary.LumiformSummaryPageUiModel

/**
 * Keeps only the choice questions of each page, sections flattened, with the responses picked in [selectedResponses].
 * Pages without any choice question are left out.
 */
fun List<PageDomainModel>.toSummaryPages(
    selectedResponses: Map<Int, List<Int>>
): List<LumiformSummaryPageUiModel> = mapNotNull { page ->
    val answers = page.items.choiceQuestions().map { it.toSummaryAnswer(selectedResponses[it.id].orEmpty()) }
    if (answers.isEmpty()) null else LumiformSummaryPageUiModel(id = page.id, title = page.title, answers = answers)
}

private fun List<PageContentDomainModel>.choiceQuestions(): List<ChoiceQuestionDomainModel> = flatMap { item ->
    when (item) {
        is SectionDomainModel -> item.items.choiceQuestions()
        is ChoiceQuestionDomainModel -> listOf(item)
        else -> emptyList()
    }
}

private fun ChoiceQuestionDomainModel.toSummaryAnswer(selectedIds: List<Int>): LumiformSummaryAnswerUiModel {
    val selected = responseSet.responses.filter { it.id in selectedIds }
    return LumiformSummaryAnswerUiModel(
        questionId = id,
        question = content,
        answers = selected.map { it.label },
        score = selected.sumOf { it.score ?: 0 }
    )
}
