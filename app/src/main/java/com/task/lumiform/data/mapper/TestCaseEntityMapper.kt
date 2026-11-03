package com.task.lumiform.data.mapper

import com.task.lumiform.data.local.ItemEntity
import com.task.lumiform.data.local.ItemType
import com.task.lumiform.data.local.ResponseEntity
import com.task.lumiform.data.local.TestCaseRows
import com.task.lumiform.data.remote.dto.ChoiceQuestionDto
import com.task.lumiform.data.remote.dto.ImageQuestionDto
import com.task.lumiform.data.remote.dto.PageContentDto
import com.task.lumiform.data.remote.dto.PageDto
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

/**
 * Flattens the pages into rows. Every item comes after its parent, so inserting them in order never breaks a
 * foreign key.
 */
fun List<PageDto>.toRows(): TestCaseRows {
    val items = mutableListOf<ItemEntity>()
    val responses = mutableListOf<ResponseEntity>()

    fun addContent(content: PageContentDto, parentId: Int, position: Int) {
        items += content.toItemEntity(parentId, position)
        when (content) {
            is SectionDto -> content.items.forEachIndexed { index, child -> addContent(child, content.id, index) }
            is ChoiceQuestionDto -> content.responseSet.responses.forEachIndexed { index, response ->
                responses += ResponseEntity(
                    questionId = content.id,
                    id = response.id,
                    position = index,
                    label = response.label,
                    score = response.score,
                )
            }
            is TextQuestionDto, is ImageQuestionDto -> Unit
        }
    }

    forEachIndexed { index, page ->
        items += ItemEntity(id = page.id, parentId = null, position = index, type = ItemType.PAGE, title = page.title)
        page.items.forEachIndexed { childIndex, child -> addContent(child, page.id, childIndex) }
    }
    return TestCaseRows(items, responses)
}

private fun PageContentDto.toItemEntity(parentId: Int, position: Int): ItemEntity = when (this) {
    is SectionDto -> ItemEntity(id, parentId, position, ItemType.SECTION, title = title)
    is TextQuestionDto -> ItemEntity(id, parentId, position, ItemType.TEXT, content = content)
    is ImageQuestionDto -> ItemEntity(id, parentId, position, ItemType.IMAGE, title = title, src = src)
    is ChoiceQuestionDto -> ItemEntity(
        id = id,
        parentId = parentId,
        position = position,
        type = ItemType.CHOICE,
        content = content,
        responseSetId = responseSet.id,
        multipleSelection = responseSet.multipleSelection,
    )
}

/**
 * Rebuilds the page tree from the rows, children in [ItemEntity.position] order.
 * A row that does not fit the tree (for example a page under a section) is skipped instead of failing the screen.
 */
fun TestCaseRows.toDomainModel(): List<PageDomainModel> = TreeBuilder(this).pages()

private class TreeBuilder(rows: TestCaseRows) {
    private val childrenByParent = rows.items.sortedBy { it.position }.groupBy { it.parentId }
    private val responsesByQuestion = rows.responses.sortedBy { it.position }.groupBy { it.questionId }

    fun pages(): List<PageDomainModel> = childrenByParent[null].orEmpty()
        .filter { it.type == ItemType.PAGE }
        .map { page -> PageDomainModel(id = page.id, title = page.title.orEmpty(), items = children(page.id)) }

    private fun children(parentId: Int): List<PageContentDomainModel> =
        childrenByParent[parentId].orEmpty().mapNotNull { it.toContent() }

    private fun ItemEntity.toContent(): PageContentDomainModel? = when (type) {
        ItemType.PAGE -> null
        ItemType.SECTION -> SectionDomainModel(id = id, title = title.orEmpty(), items = children(id))
        ItemType.TEXT -> TextQuestionDomainModel(id = id, content = content.orEmpty())
        ItemType.IMAGE -> ImageQuestionDomainModel(id = id, src = src.orEmpty(), title = title.orEmpty())
        ItemType.CHOICE -> responseSetId?.let { responseSetId ->
            ChoiceQuestionDomainModel(
                id = id,
                content = content.orEmpty(),
                responseSet = ResponseSetDomainModel(
                    id = responseSetId,
                    multipleSelection = multipleSelection,
                    responses = responsesByQuestion[id].orEmpty().map { it.toDomainModel() }
                )
            )
        }
    }
}

private fun ResponseEntity.toDomainModel() = ResponseOptionDomainModel(
    id = id,
    label = label,
    score = score,
    selected = selected
)
