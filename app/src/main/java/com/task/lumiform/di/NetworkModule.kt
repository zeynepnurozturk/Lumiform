package com.task.lumiform.di

import android.content.Context
import com.task.lumiform.data.remote.mock.MockNetworkResponseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Singleton
    @Provides
    fun provideMockNetworkResponseProvider(
        @ApplicationContext context: Context,
        json: Json
    ): MockNetworkResponseProvider = MockNetworkResponseProvider(context = context, json = json)
}
