package com.example.kotlin_asr_with_ncnn.di

import com.example.kotlin_asr_with_ncnn.data.repository.TranscriptionHistoryRepositoryImpl
import com.example.kotlin_asr_with_ncnn.domain.repository.TranscriptionHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTranscriptionHistoryRepository(
        impl: TranscriptionHistoryRepositoryImpl,
    ): TranscriptionHistoryRepository
}
