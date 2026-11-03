package com.task.lumiform.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PageDto(
    val id: Int,
    val title: String? = null,
    val items: List<PageContentDto> = emptyList()
)

/**
 * Anything a Page or Section can contain: a Section or a Question. The concrete subtype is chosen by "type".
 */
@Serializable
sealed interface PageContentDto {
    val id: Int
}

@Serializable
@SerialName("section")
data class SectionDto(
    override val id: Int,
    val title: String,
    val items: List<PageContentDto> = emptyList()
) : PageContentDto

@Serializable
sealed interface QuestionDto : PageContentDto

@Serializable
@SerialName("text")
data class TextQuestionDto(
    override val id: Int,
    val content: String? = null
) : QuestionDto

@Serializable
@SerialName("image")
data class ImageQuestionDto(
    override val id: Int,
    val src: String? = null,
    val title: String? = null
) : QuestionDto

@Serializable
@SerialName("choice")
data class ChoiceQuestionDto(
    override val id: Int,
    val content: String? = null,
    @SerialName("response_set") val responseSet: ResponseSetDto
) : QuestionDto

@Serializable
data class ResponseSetDto(
    val id: Int,
    @SerialName("multiple_selection") val multipleSelection: Boolean = false,
    val responses: List<ResponseOptionDto> = emptyList()
)

@Serializable
data class ResponseOptionDto(
    val id: Int,
    val label: String,
    val score: Int? = null
)
