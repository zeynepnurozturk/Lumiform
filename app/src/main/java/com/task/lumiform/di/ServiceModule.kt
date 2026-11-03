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

    // To use the bundled mock JSON instead of the gist, comment out the binding above
    // and uncomment this one (and its MockLumiformTestService import).
    // @Binds
    // fun bindsLumiformTestService(
    //     lumiformTestService: MockLumiformTestService
    // ): LumiformTestService
}
