package com.example.kotlin_asr_with_ncnn.di

import com.example.kotlin_asr_with_ncnn.core.media.AudioRecorder
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import com.example.kotlin_asr_with_ncnn.data.repository.ASRRepositoryImpl
import com.example.kotlin_asr_with_ncnn.domain.repository.ASRRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideASRRepository(
        nativeBridge: NcnnNativeBridge,
        audioRecorder: AudioRecorder
    ): ASRRepository {
        return ASRRepositoryImpl(nativeBridge, audioRecorder)
    }
}
