package com.example.kotlin_asr_with_ncnn.data.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "asr_app.db")
            .build()

    @Provides
    fun provideTranscriptionHistoryDao(db: AppDatabase): TranscriptionHistoryDao =
        db.transcriptionHistoryDao()
}
