package com.example.kotlin_asr_with_ncnn.domain.usecase

import com.example.kotlin_asr_with_ncnn.domain.repository.TranscriptionHistoryRepository
import javax.inject.Inject

class DeleteTranscriptionHistoryEntryUseCase @Inject constructor(
    private val repository: TranscriptionHistoryRepository,
) {
    suspend operator fun invoke(id: String) = repository.remove(id)
}
