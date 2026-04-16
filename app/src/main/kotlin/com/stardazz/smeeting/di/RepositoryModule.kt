package com.stardazz.smeeting.di

import com.stardazz.smeeting.data.repository.ASRRepositoryImpl
import com.stardazz.smeeting.data.repository.LLMRepositoryImpl
import com.stardazz.smeeting.data.repository.TranscriptionHistoryRepositoryImpl
import com.stardazz.smeeting.domain.repository.ASRRepository
import com.stardazz.smeeting.domain.repository.LLMRepository
import com.stardazz.smeeting.domain.repository.TranscriptionHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindASRRepository(
        impl: ASRRepositoryImpl,
    ): ASRRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptionHistoryRepository(
        impl: TranscriptionHistoryRepositoryImpl,
    ): TranscriptionHistoryRepository

    @Binds
    @Singleton
    abstract fun bindLLMRepository(
        impl: LLMRepositoryImpl,
    ): LLMRepository
}
