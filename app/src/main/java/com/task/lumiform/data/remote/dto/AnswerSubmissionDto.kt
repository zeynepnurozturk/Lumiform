package com.task.lumiform.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The answers of one page. Same structure as the page itself, with the selected responses marked.
 */
@Serializable
data class AnswerSubmissionDto(
    val id: Int,
    val title: String = "",
    val items: List<SubmissionContentDto> = emptyList()
)

@Serializable
sealed interface SubmissionContentDto {
    val id: Int
}

@Serializable
@SerialName("section")
data class SubmissionSectionDto(
    override val id: Int,
    val title: String,
    val items: List<SubmissionContentDto> = emptyList()
) : SubmissionContentDto

@Serializable
@SerialName("text")
data class SubmissionTextQuestionDto(
    override val id: Int,
    val content: String = ""
) : SubmissionContentDto

@Serializable
@SerialName("image")
data class SubmissionImageQuestionDto(
    override val id: Int,
    val src: String = "",
    val title: String = ""
) : SubmissionContentDto

@Serializable
@SerialName("choice")
data class SubmissionChoiceQuestionDto(
    override val id: Int,
    val content: String = "",
    @SerialName("response_set") val responseSet: SubmissionResponseSetDto
) : SubmissionContentDto

@Serializable
data class SubmissionResponseSetDto(
    val id: Int,
    @SerialName("multiple_selection") val multipleSelection: Boolean = false,
    val responses: List<SubmissionResponseOptionDto> = emptyList()
)

@Serializable
data class SubmissionResponseOptionDto(
    val id: Int,
    val label: String = "",
    val score: Int? = null,
    val selected: Boolean = false
)
