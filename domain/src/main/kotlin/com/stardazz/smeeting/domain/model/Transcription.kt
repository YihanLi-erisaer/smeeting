package com.stardazz.smeeting.domain.model

data class Transcription(
    val id: String,
    val text: String,
    val confidence: Float,
    val timestamp: Long,
    val isFinal: Boolean
)