package com.task.lumiform.data.sync

import androidx.work.WorkInfo.State
import com.task.lumiform.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusTest {

    @Test
    fun `nothing queued and nothing unsynced is Synced`() {
        assertEquals(SyncStatus.Synced, syncStatusOf(emptyList(), unsyncedPages = 0, isOnline = true))
    }

    @Test
    fun `finished work is ignored`() {
        val work = listOf(SyncWork(State.SUCCEEDED, 0), SyncWork(State.CANCELLED, 2))

        assertEquals(SyncStatus.Synced, syncStatusOf(work, unsyncedPages = 0, isOnline = false))
    }

    @Test
    fun `unsynced pages with nothing queued are SavedOnDevice`() {
        assertEquals(SyncStatus.SavedOnDevice, syncStatusOf(emptyList(), unsyncedPages = 2, isOnline = true))
    }

    @Test
    fun `queued work while offline is WaitingForNetwork`() {
        val work = listOf(SyncWork(State.ENQUEUED, 0), SyncWork(State.BLOCKED, 0))

        assertEquals(SyncStatus.WaitingForNetwork, syncStatusOf(work, unsyncedPages = 2, isOnline = false))
    }

    @Test
    fun `queued work while online is Sending`() {
        val work = listOf(SyncWork(State.ENQUEUED, 0))

        assertEquals(SyncStatus.Sending, syncStatusOf(work, unsyncedPages = 1, isOnline = true))
    }

    @Test
    fun `running work is Sending even if the connection just dropped`() {
        val work = listOf(SyncWork(State.RUNNING, 0), SyncWork(State.BLOCKED, 0))

        assertEquals(SyncStatus.Sending, syncStatusOf(work, unsyncedPages = 2, isOnline = false))
    }

    @Test
    fun `work waiting after a failed attempt is Retrying`() {
        val work = listOf(SyncWork(State.ENQUEUED, 1), SyncWork(State.BLOCKED, 0))

        assertEquals(SyncStatus.Retrying, syncStatusOf(work, unsyncedPages = 2, isOnline = true))
    }

    @Test
    fun `a retry while offline is WaitingForNetwork`() {
        val work = listOf(SyncWork(State.ENQUEUED, 3))

        assertEquals(SyncStatus.WaitingForNetwork, syncStatusOf(work, unsyncedPages = 1, isOnline = false))
    }
}
