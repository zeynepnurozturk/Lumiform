package com.task.lumiform.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class LumiformItemDto {
    abstract val id: Int

    @Serializable
    @SerialName("page")
    data class Page(
        override val id: Int,
        val title: String? = null,
        val items: List<LumiformItemDto> = emptyList()
    ) : LumiformItemDto()

    @Serializable
    @SerialName("section")
    data class Section(
        override val id: Int,
        val title: String? = null,
        val items: List<LumiformItemDto> = emptyList()
    ) : LumiformItemDto()

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: Int,
        val content: String? = null
    ) : LumiformItemDto()

    @Serializable
    @SerialName("image")
    data class Image(
        override val id: Int,
        val src: String? = null,
        val title: String? = null
    ) : LumiformItemDto()

    @Serializable
    @SerialName("choice")
    data class Choice(
        override val id: Int,
        val content: String? = null,
        @SerialName("response_set") val responseSet: ResponseSetDto? = null
    ) : LumiformItemDto()
}

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
