package com.task.lumiform.domain.model

sealed class LumiformItemDomainModel {
    abstract val id: Int

    data class Page(
        override val id: Int,
        val title: String = "",
        val items: List<LumiformItemDomainModel> = emptyList()
    ) : LumiformItemDomainModel()

    data class Section(
        override val id: Int,
        val title: String = "",
        val items: List<LumiformItemDomainModel> = emptyList()
    ) : LumiformItemDomainModel()

    data class Text(
        override val id: Int,
        val content: String = ""
    ) : LumiformItemDomainModel()

    data class Image(
        override val id: Int,
        val src: String = "",
        val title: String = ""
    ) : LumiformItemDomainModel()

    data class Choice(
        override val id: Int,
        val content: String = "",
        val responseSet: ResponseSetDomainModel? = null
    ) : LumiformItemDomainModel()
}

data class ResponseSetDomainModel(
    val id: Int,
    val multipleSelection: Boolean = false,
    val responses: List<ResponseOptionDomainModel> = emptyList()
)

data class ResponseOptionDomainModel(
    val id: Int,
    val label: String = "",
    val score: Int? = null
)
