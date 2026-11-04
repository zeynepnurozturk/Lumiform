package com.task.lumiform.data.sync

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.task.lumiform.data.local.TestCaseDao
import com.task.lumiform.domain.model.SyncStatus
import com.task.lumiform.domain.repository.SyncStatusRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Builds the [SyncStatus] from the queued sync work, the pages Room still has unsynced and the connection.
 */
class SyncStatusRepositoryImpl @Inject internal constructor(
    private val workManager: WorkManager,
    private val testCaseDao: TestCaseDao,
    private val networkMonitor: NetworkMonitor,
) : SyncStatusRepository {

    override fun observeSyncStatus(): Flow<SyncStatus> = combine(
        workManager.getWorkInfosForUniqueWorkFlow(AnswerSyncWorker.WORK_NAME),
        testCaseDao.observeUnsyncedPageCount(),
        networkMonitor.isOnline,
    ) { work, unsyncedPages, isOnline ->
        syncStatusOf(
            work = work.map { SyncWork(it.state, it.runAttemptCount) },
            unsyncedPages = unsyncedPages,
            isOnline = isOnline,
        )
    }.distinctUntilChanged()
}

internal data class SyncWork(val state: WorkInfo.State, val runAttemptCount: Int)

/**
 * Work that is running wins, then a queue waiting for a connection, then a retry waiting for its backoff.
 * Queued work while online is about to run, so it counts as sending. With nothing queued, unsynced pages
 * mean answers that are only saved on the device.
 */
internal fun syncStatusOf(work: List<SyncWork>, unsyncedPages: Int, isOnline: Boolean): SyncStatus {
    val pending = work.filterNot { it.state.isFinished }
    return when {
        pending.any { it.state == WorkInfo.State.RUNNING } -> SyncStatus.Sending
        pending.isNotEmpty() && !isOnline -> SyncStatus.WaitingForNetwork
        pending.any { it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount > 0 } -> SyncStatus.Retrying
        pending.isNotEmpty() -> SyncStatus.Sending
        unsyncedPages > 0 -> SyncStatus.SavedOnDevice
        else -> SyncStatus.Synced
    }
}
