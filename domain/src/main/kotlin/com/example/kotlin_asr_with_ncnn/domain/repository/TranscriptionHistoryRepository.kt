package com.example.kotlin_asr_with_ncnn.domain.repository

import com.example.kotlin_asr_with_ncnn.domain.model.TranscriptionHistoryEntry
import kotlinx.coroutines.flow.Flow

interface TranscriptionHistoryRepository {
    val entries: Flow<List<TranscriptionHistoryEntry>>

    suspend fun append(text: String)

    suspend fun remove(id: String)
}
