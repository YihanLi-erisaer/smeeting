package com.stardazz.smeeting.domain.usecase

import com.stardazz.smeeting.domain.model.Transcription
import com.stardazz.smeeting.domain.repository.ASRRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StartASRUseCase @Inject constructor(private val repository: ASRRepository) {
    operator fun invoke(): Flow<Transcription> {
        return repository.startListening()
    }
}