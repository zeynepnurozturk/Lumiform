package com.task.lumiform.data.mapper

import com.task.lumiform.data.remote.dto.AnswerSubmissionDto
import com.task.lumiform.data.remote.dto.SubmissionChoiceQuestionDto
import com.task.lumiform.data.remote.dto.SubmissionContentDto
import com.task.lumiform.data.remote.dto.SubmissionImageQuestionDto
import com.task.lumiform.data.remote.dto.SubmissionResponseOptionDto
import com.task.lumiform.data.remote.dto.SubmissionResponseSetDto
import com.task.lumiform.data.remote.dto.SubmissionSectionDto
import com.task.lumiform.data.remote.dto.SubmissionTextQuestionDto
import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.ImageQuestionDomainModel
import com.task.lumiform.domain.model.PageContentDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.domain.model.TextQuestionDomainModel

/**
 * The whole page with its current selections, in the same structure as the page.
 */
fun PageDomainModel.toSubmissionDto() = AnswerSubmissionDto(
    id = id,
    title = title,
    items = items.map { it.toSubmissionDto() }
)

private fun PageContentDomainModel.toSubmissionDto(): SubmissionContentDto = when (this) {
    is SectionDomainModel -> SubmissionSectionDto(id, title, items.map { it.toSubmissionDto() })
    is TextQuestionDomainModel -> SubmissionTextQuestionDto(id, content)
    is ImageQuestionDomainModel -> SubmissionImageQuestionDto(id, src, title)
    is ChoiceQuestionDomainModel -> SubmissionChoiceQuestionDto(
        id = id,
        content = content,
        responseSet = SubmissionResponseSetDto(
            id = responseSet.id,
            multipleSelection = responseSet.multipleSelection,
            responses = responseSet.responses.map {
                SubmissionResponseOptionDto(it.id, it.label, it.score, it.selected)
            }
        )
    )
}
