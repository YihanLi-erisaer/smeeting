package com.example.kotlin_asr_with_ncnn.domain.model

data class TranscriptionHistoryEntry(
    val id: String,
    val text: String,
    val createdAtMillis: Long,
)
