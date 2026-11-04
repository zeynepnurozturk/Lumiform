package com.task.lumiform.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnswerSyncWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun succeedsWhenThePageIsSent() = runBlocking {
        val repository = FakeRepository(syncResult = UseCaseResult.Success(Unit))

        val result = buildWorker(repository, workDataOf("page_id" to 1)).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(listOf(1), repository.syncedPageIds)
    }

    @Test
    fun retriesWhenTheSendFails() = runBlocking {
        val repository = FakeRepository(syncResult = UseCaseResult.Error(UseCaseError.NetworkError("offline")))

        val result = buildWorker(repository, workDataOf("page_id" to 1)).doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun failsWithoutAPageId() = runBlocking {
        val repository = FakeRepository(syncResult = UseCaseResult.Success(Unit))

        val result = buildWorker(repository, workDataOf()).doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(emptyList<Int>(), repository.syncedPageIds)
    }

    @Test
    fun queuedPagesAreSentInOrder() {
        val repository = FakeRepository(syncResult = UseCaseResult.Success(Unit))
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(FakeWorkerFactory(repository))
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        val workManager = WorkManager.getInstance(context)

        AnswerSyncWorker.enqueue(workManager, 1)
        AnswerSyncWorker.enqueue(workManager, 2)

        assertEquals(listOf(1, 2), repository.syncedPageIds)
        val states = workManager.getWorkInfosForUniqueWork(AnswerSyncWorker.WORK_NAME).get().map { it.state }
        assertEquals(listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.SUCCEEDED), states)
    }

    private fun buildWorker(repository: FakeRepository, input: androidx.work.Data) =
        TestListenableWorkerBuilder<AnswerSyncWorker>(context)
            .setInputData(input)
            .setWorkerFactory(FakeWorkerFactory(repository))
            .build()

    private class FakeWorkerFactory(private val repository: LumiformTestRepository) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ) = AnswerSyncWorker(appContext, workerParameters, repository)
    }

    private class FakeRepository(private val syncResult: UseCaseResult<Unit>) : LumiformTestRepository {
        val syncedPageIds = mutableListOf<Int>()
        override suspend fun getTestCase(): UseCaseResult<LumiformTestUseCase.Result> =
            UseCaseResult.Success(LumiformTestUseCase.Result(emptyList()))
        override suspend fun getAnswers() = emptyMap<Int, List<Int>>()
        override suspend fun saveAnswer(pageId: Int, questionId: Int, selectedResponseIds: List<Int>) = Unit
        override fun queuePageForSync(pageId: Int) = Unit
        override suspend fun syncPageAnswers(pageId: Int): UseCaseResult<Unit> {
            syncedPageIds += pageId
            return syncResult
        }
        override suspend fun submitAll() = UseCaseResult.Success(Unit)
    }
}
