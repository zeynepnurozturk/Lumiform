package com.task.lumiform.data.mapper

import com.task.lumiform.data.remote.dto.LumiformItemDto
import com.task.lumiform.data.remote.dto.ResponseOptionDto
import com.task.lumiform.data.remote.dto.ResponseSetDto
import com.task.lumiform.domain.model.LumiformItemDomainModel
import com.task.lumiform.domain.model.ResponseOptionDomainModel
import com.task.lumiform.domain.model.ResponseSetDomainModel

fun LumiformItemDto.toDomainModel(): LumiformItemDomainModel = when (this) {
    is LumiformItemDto.Page -> LumiformItemDomainModel.Page(
        id = id,
        title = title.orEmpty(),
        items = items.map { it.toDomainModel() }
    )
    is LumiformItemDto.Section -> LumiformItemDomainModel.Section(
        id = id,
        title = title.orEmpty(),
        items = items.map { it.toDomainModel() }
    )
    is LumiformItemDto.Text -> LumiformItemDomainModel.Text(
        id = id,
        content = content.orEmpty()
    )
    is LumiformItemDto.Image -> LumiformItemDomainModel.Image(
        id = id,
        src = src.orEmpty(),
        title = title.orEmpty()
    )
    is LumiformItemDto.Choice -> LumiformItemDomainModel.Choice(
        id = id,
        content = content.orEmpty(),
        responseSet = responseSet?.toDomainModel()
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
