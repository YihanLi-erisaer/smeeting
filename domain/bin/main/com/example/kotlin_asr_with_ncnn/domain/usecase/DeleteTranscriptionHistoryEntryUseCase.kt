package com.example.kotlin_asr_with_ncnn.domain.usecase

import com.example.kotlin_asr_with_ncnn.domain.repository.TranscriptionHistoryRepository

class DeleteTranscriptionHistoryEntryUseCase(
    private val repository: TranscriptionHistoryRepository,
) {
    suspend operator fun invoke(id: String) = repository.remove(id)
}
