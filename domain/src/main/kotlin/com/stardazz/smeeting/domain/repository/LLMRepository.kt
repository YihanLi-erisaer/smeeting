package com.stardazz.smeeting.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LLMRepository {
    val isModelReady: StateFlow<Boolean>
    fun summarize(text: String): Flow<String>
    suspend fun abortGeneration()
}
