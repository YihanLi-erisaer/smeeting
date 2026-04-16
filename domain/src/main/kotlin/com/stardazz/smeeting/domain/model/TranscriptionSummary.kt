package com.stardazz.smeeting.domain.model

data class TranscriptionSummary(
    val keyPoints: List<String>,
    val actionItems: List<String>,
    val briefSummary: String,
    val generatedAtMillis: Long,
)
