package com.task.lumiform.domain.repository

import com.task.lumiform.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncStatusRepository {
    /**
     * The current [SyncStatus], updated whenever an answer is saved, a page is queued or sent, or the connection
     * changes.
     */
    fun observeSyncStatus(): Flow<SyncStatus>
}
