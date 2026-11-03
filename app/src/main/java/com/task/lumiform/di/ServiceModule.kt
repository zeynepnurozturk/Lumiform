package com.task.lumiform.di

import com.task.lumiform.data.remote.service.LumiformTestService
import com.task.lumiform.data.remote.service.LumiformTestServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ServiceModule {

    @Binds
    fun bindsLumiformTestService(
        lumiformTestService: LumiformTestServiceImpl
    ): LumiformTestService
}
