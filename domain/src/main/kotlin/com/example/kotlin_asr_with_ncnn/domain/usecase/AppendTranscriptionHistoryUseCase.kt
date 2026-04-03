package com.example.kotlin_asr_with_ncnn.domain.usecase

import com.example.kotlin_asr_with_ncnn.domain.repository.TranscriptionHistoryRepository

class AppendTranscriptionHistoryUseCase(
    private val repository: TranscriptionHistoryRepository,
) {
    suspend operator fun invoke(text: String) {
        val t = text.trim()
        if (t.isNotEmpty()) repository.append(t)
    }
}
