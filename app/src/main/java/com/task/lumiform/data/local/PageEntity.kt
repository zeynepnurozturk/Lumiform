package com.task.lumiform.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One page of the last successful API response, kept as the JSON it arrived in so the test case can be shown
 * offline. The user's selections live in that JSON too (the `selected` flag of each response).
 * [position] keeps the order the server sent the pages in.
 * [isSynced] turns false on every answer change and true once that exact version ([updatedAt]) has been sent.
 */
@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey val id: Int,
    val position: Int,
    val json: String,
    @ColumnInfo(defaultValue = "1") val isSynced: Boolean = true,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
)
