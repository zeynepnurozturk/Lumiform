package com.task.lumiform.domain.model

data class PageDomainModel(
    val id: Int,
    val title: String = "",
    val items: List<PageContentDomainModel> = emptyList()
)

/**
 * Anything a Page or Section can contain: a Section or a Question.
 */
sealed interface PageContentDomainModel {
    val id: Int
}

data class SectionDomainModel(
    override val id: Int,
    val title: String,
    val items: List<PageContentDomainModel> = emptyList()
) : PageContentDomainModel

sealed interface QuestionDomainModel : PageContentDomainModel

data class TextQuestionDomainModel(
    override val id: Int,
    val content: String = ""
) : QuestionDomainModel

data class ImageQuestionDomainModel(
    override val id: Int,
    val src: String = "",
    val title: String = ""
) : QuestionDomainModel

data class ChoiceQuestionDomainModel(
    override val id: Int,
    val content: String = "",
    val responseSet: ResponseSetDomainModel
) : QuestionDomainModel

data class ResponseSetDomainModel(
    val id: Int,
    val multipleSelection: Boolean = false,
    val responses: List<ResponseOptionDomainModel> = emptyList()
)

data class ResponseOptionDomainModel(
    val id: Int,
    val label: String = "",
    val score: Int? = null,
    val selected: Boolean = false
)
