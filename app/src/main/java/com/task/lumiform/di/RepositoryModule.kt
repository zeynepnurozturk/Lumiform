package com.task.lumiform.di

import com.task.lumiform.data.repo.LumiformTestRepositoryImpl
import com.task.lumiform.data.sync.SyncStatusRepositoryImpl
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.repository.SyncStatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindsLumiformTestRepository(
        lumiformTestRepository: LumiformTestRepositoryImpl
    ): LumiformTestRepository

    @Binds
    fun bindsSyncStatusRepository(
        syncStatusRepository: SyncStatusRepositoryImpl
    ): SyncStatusRepository
}
