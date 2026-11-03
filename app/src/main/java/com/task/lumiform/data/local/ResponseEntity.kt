package com.task.lumiform.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * One selectable response of a choice question. [selected] is the user's answer.
 * The key includes [questionId] because the API does not promise response ids are unique across questions.
 */
@Entity(
    tableName = "responses",
    primaryKeys = ["questionId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class ResponseEntity(
    val questionId: Int,
    val id: Int,
    val position: Int,
    val label: String,
    val score: Int? = null,
    val selected: Boolean = false,
)
