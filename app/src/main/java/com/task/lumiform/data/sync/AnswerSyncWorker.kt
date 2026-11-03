package com.task.lumiform.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.task.lumiform.domain.repository.LumiformTestRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Sends one page's unsynced answers. It retries with backoff if the send fails.
 */
@HiltWorker
class AnswerSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: LumiformTestRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pageId = inputData.getInt(KEY_PAGE_ID, NO_PAGE_ID)
        if (pageId == NO_PAGE_ID) return Result.failure()
        println("[AnswerSync] page $pageId: attempt ${runAttemptCount + 1}")

        return if (repository.syncPageAnswers(pageId).isSuccessful()) {
            println("[AnswerSync] page $pageId: sent")
            Result.success()
        } else {
            println("[AnswerSync] page $pageId: send failed, will retry")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "answer_sync"
        private const val KEY_PAGE_ID = "page_id"
        private const val NO_PAGE_ID = -1
        private const val BACKOFF_SECONDS = 10L

        /**
         * Pages are sent one after another in the order they were queued, under a single unique work name.
         */
        fun enqueue(workManager: WorkManager, pageId: Int) {
            val request = OneTimeWorkRequestBuilder<AnswerSyncWorker>()
                .setInputData(workDataOf(KEY_PAGE_ID to pageId))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            println("[AnswerSync] page $pageId: queued")
        }
    }
}
