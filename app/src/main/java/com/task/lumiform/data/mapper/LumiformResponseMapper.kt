package com.task.lumiform.data.mapper

import com.task.lumiform.data.remote.dto.ChoiceQuestionDto
import com.task.lumiform.data.remote.dto.ImageQuestionDto
import com.task.lumiform.data.remote.dto.PageContentDto
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.dto.ResponseOptionDto
import com.task.lumiform.data.remote.dto.ResponseSetDto
import com.task.lumiform.data.remote.dto.SectionDto
import com.task.lumiform.data.remote.dto.TextQuestionDto
import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.ImageQuestionDomainModel
import com.task.lumiform.domain.model.PageContentDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.ResponseOptionDomainModel
import com.task.lumiform.domain.model.ResponseSetDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.domain.model.TextQuestionDomainModel

fun PageDto.toDomainModel() = PageDomainModel(
    id = id,
    title = title.orEmpty(),
    items = items.map { it.toDomainModel() }
)

fun PageContentDto.toDomainModel(): PageContentDomainModel = when (this) {
    is SectionDto -> SectionDomainModel(
        id = id,
        title = title,
        items = items.map { it.toDomainModel() }
    )
    is TextQuestionDto -> TextQuestionDomainModel(
        id = id,
        content = content.orEmpty()
    )
    is ImageQuestionDto -> ImageQuestionDomainModel(
        id = id,
        src = src.orEmpty(),
        title = title.orEmpty()
    )
    is ChoiceQuestionDto -> ChoiceQuestionDomainModel(
        id = id,
        content = content.orEmpty(),
        responseSet = responseSet.toDomainModel()
    )
}

fun ResponseSetDto.toDomainModel() = ResponseSetDomainModel(
    id = id,
    multipleSelection = multipleSelection,
    responses = responses.map { it.toDomainModel() }
)

fun ResponseOptionDto.toDomainModel() = ResponseOptionDomainModel(
    id = id,
    label = label,
    score = score
)
