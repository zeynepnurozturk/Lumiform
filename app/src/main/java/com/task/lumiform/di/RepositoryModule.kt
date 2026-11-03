package com.task.lumiform.di

import com.task.lumiform.data.repo.LumiformTestRepositoryImpl
import com.task.lumiform.domain.repository.LumiformTestRepository
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
}
