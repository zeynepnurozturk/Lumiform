package com.task.lumiform.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One node of the cached test case: a page, a section or a question, told apart by [type].
 * [parentId] is the page or section it sits in (null for pages) and [position] its order among its siblings,
 * so the whole tree is rebuilt from the rows. Each type only fills its own columns, the rest stay null.
 * [isSynced] and [updatedAt] are only used on pages: [isSynced] turns false on every answer change on the page
 * and true once that exact version ([updatedAt]) has been sent.
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("parentId")],
)
data class ItemEntity(
    @PrimaryKey val id: Int,
    val parentId: Int?,
    val position: Int,
    val type: ItemType,
    val title: String? = null,
    val content: String? = null,
    val src: String? = null,
    val responseSetId: Int? = null,
    val multipleSelection: Boolean = false,
    val isSynced: Boolean = true,
    val updatedAt: Long = 0,
)

enum class ItemType { PAGE, SECTION, TEXT, IMAGE, CHOICE }
