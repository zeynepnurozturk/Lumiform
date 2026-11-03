package com.task.lumiform.ui.model

import com.task.lumiform.domain.model.ResponseOptionDomainModel

data class LumiformHomePageUiModel(
    val id: Int,
    val title: String,
    val items: List<LumiformHomeItemUiModel>
)

sealed interface LumiformHomeItemUiModel {
    val id: Int

    data class Section(
        override val id: Int,
        val title: String,
        val items: List<LumiformHomeItemUiModel>
    ) : LumiformHomeItemUiModel

    data class Text(override val id: Int, val content: String) : LumiformHomeItemUiModel

    data class Image(override val id: Int, val src: String, val title: String) : LumiformHomeItemUiModel

    data class Choice(
        override val id: Int,
        val content: String,
        val selectionType: SelectionType,
        val responses: List<ResponseOptionDomainModel>
    ) : LumiformHomeItemUiModel {
        /**
         * [Single] allows one response at a time (radio buttons), [Multiple] allows several (checkboxes).
         */
        enum class SelectionType { Single, Multiple }
    }
}
