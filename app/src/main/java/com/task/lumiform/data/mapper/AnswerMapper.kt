package com.task.lumiform.data.mapper

import com.task.lumiform.data.remote.dto.AnswerSubmissionDto
import com.task.lumiform.data.remote.dto.ChoiceQuestionDto
import com.task.lumiform.data.remote.dto.ImageQuestionDto
import com.task.lumiform.data.remote.dto.PageContentDto
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.dto.SectionDto
import com.task.lumiform.data.remote.dto.SubmissionChoiceQuestionDto
import com.task.lumiform.data.remote.dto.SubmissionContentDto
import com.task.lumiform.data.remote.dto.SubmissionImageQuestionDto
import com.task.lumiform.data.remote.dto.SubmissionResponseOptionDto
import com.task.lumiform.data.remote.dto.SubmissionResponseSetDto
import com.task.lumiform.data.remote.dto.SubmissionSectionDto
import com.task.lumiform.data.remote.dto.SubmissionTextQuestionDto
import com.task.lumiform.data.remote.dto.TextQuestionDto

/**
 * The selected response ids per choice question id, in the order the responses are listed.
 */
fun PageDto.selectedResponses(): Map<Int, List<Int>> =
    choiceQuestions().associate { question ->
        question.id to question.responseSet.responses.filter { it.selected }.map { it.id }
    }

/**
 * Marks the responses in [selections] as selected, per choice question id. Questions that are not in
 * [selections] stay as they are.
 */
fun PageDto.withSelections(selections: Map<Int, Collection<Int>>): PageDto =
    copy(items = items.map { it.withSelections(selections) })

/**
 * The whole page with its current selections, in the same structure as the page.
 */
fun PageDto.toSubmissionDto() = AnswerSubmissionDto(
    id = id,
    title = title.orEmpty(),
    items = items.map { it.toSubmissionDto() }
)

private fun PageContentDto.toSubmissionDto(): SubmissionContentDto = when (this) {
    is SectionDto -> SubmissionSectionDto(id, title, items.map { it.toSubmissionDto() })
    is TextQuestionDto -> SubmissionTextQuestionDto(id, content.orEmpty())
    is ImageQuestionDto -> SubmissionImageQuestionDto(id, src.orEmpty(), title.orEmpty())
    is ChoiceQuestionDto -> SubmissionChoiceQuestionDto(
        id = id,
        content = content.orEmpty(),
        responseSet = SubmissionResponseSetDto(
            id = responseSet.id,
            multipleSelection = responseSet.multipleSelection,
            responses = responseSet.responses.map {
                SubmissionResponseOptionDto(it.id, it.label, it.score, it.selected)
            }
        )
    )
}

private fun PageDto.choiceQuestions(): List<ChoiceQuestionDto> =
    items.flatMap { it.choiceQuestions() }

private fun PageContentDto.choiceQuestions(): List<ChoiceQuestionDto> = when (this) {
    is SectionDto -> items.flatMap { it.choiceQuestions() }
    is ChoiceQuestionDto -> listOf(this)
    is TextQuestionDto, is ImageQuestionDto -> emptyList()
}

private fun PageContentDto.withSelections(selections: Map<Int, Collection<Int>>): PageContentDto = when (this) {
    is SectionDto -> copy(items = items.map { it.withSelections(selections) })
    is ChoiceQuestionDto -> selections[id]?.let { selected ->
        copy(
            responseSet = responseSet.copy(
                responses = responseSet.responses.map { it.copy(selected = it.id in selected) }
            )
        )
    } ?: this
    is TextQuestionDto, is ImageQuestionDto -> this
}
