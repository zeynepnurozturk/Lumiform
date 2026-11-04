package com.task.lumiform.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.task.lumiform.data.local.LumiformDatabase
import com.task.lumiform.data.local.TestCaseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideLumiformDatabase(
        @ApplicationContext context: Context
    ): LumiformDatabase = Room.databaseBuilder(context, LumiformDatabase::class.java, LumiformDatabase.DATABASE_NAME)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()

    @Provides
    fun provideTestCaseDao(database: LumiformDatabase): TestCaseDao = database.testCaseDao()

    @Singleton
    @Provides
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}
