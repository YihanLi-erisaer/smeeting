package com.example.kotlin_asr_with_ncnn.domain.usecase

import com.example.kotlin_asr_with_ncnn.domain.model.TranscriptionHistoryEntry
import com.example.kotlin_asr_with_ncnn.domain.repository.TranscriptionHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTranscriptionHistoryUseCase @Inject constructor(
    private val repository: TranscriptionHistoryRepository,
) {
    operator fun invoke(): Flow<List<TranscriptionHistoryEntry>> = repository.entries
}
