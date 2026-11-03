package com.task.lumiform.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.task.lumiform.data.local.LumiformDatabase
import com.task.lumiform.data.local.PageDao
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
        .addMigrations(LumiformDatabase.MIGRATION_1_2, LumiformDatabase.MIGRATION_2_3)
        .build()

    @Provides
    fun providePageDao(database: LumiformDatabase): PageDao = database.pageDao()

    @Singleton
    @Provides
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}
