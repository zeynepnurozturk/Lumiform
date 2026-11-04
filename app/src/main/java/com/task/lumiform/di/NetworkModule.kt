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
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
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

        // Limits for a single attempt. LumiformTestServiceImpl retries a failed attempt.
        @Singleton
        @Provides
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        @Singleton
        @Provides
        fun provideLumiformApi(json: Json, okHttpClient: OkHttpClient): LumiformApi = Retrofit.Builder()
            .baseUrl(LumiformApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LumiformApi::class.java)

        @Singleton
        @Provides
        fun provideMockNetworkResponseProvider(
            @ApplicationContext context: Context,
            json: Json
        ): MockNetworkResponseProvider = MockNetworkResponseProvider(context = context, json = json)

        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 10L
        private const val CALL_TIMEOUT_SECONDS = 5L
    }
}
