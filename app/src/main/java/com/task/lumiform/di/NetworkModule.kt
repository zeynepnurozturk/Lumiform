package com.task.lumiform.di

import android.content.Context
import com.task.lumiform.data.remote.mock.MockNetworkResponseProvider
import com.task.lumiform.data.remote.service.LumiformApi
import com.task.lumiform.data.remote.service.LumiformTestService
import com.task.lumiform.data.remote.service.LumiformTestServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindsLumiformTestService(
        lumiformTestService: LumiformTestServiceImpl
    ): LumiformTestService

    // To use the bundled mock JSON instead of the gist, comment out the binding above
    // and uncomment this one (and its MockLumiformTestService import).
    // @Binds
    // abstract fun bindsLumiformTestService(
    //     lumiformTestService: MockLumiformTestService
    // ): LumiformTestService

    companion object {

        @Singleton
        @Provides
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        @Singleton
        @Provides
        fun provideLumiformApi(json: Json): LumiformApi = Retrofit.Builder()
            .baseUrl(LumiformApi.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LumiformApi::class.java)

        @Singleton
        @Provides
        fun provideMockNetworkResponseProvider(
            @ApplicationContext context: Context,
            json: Json
        ): MockNetworkResponseProvider = MockNetworkResponseProvider(context = context, json = json)
    }
}
