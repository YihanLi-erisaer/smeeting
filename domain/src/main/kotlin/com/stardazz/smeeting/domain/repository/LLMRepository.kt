package com.stardazz.smeeting.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LLMRepository {
    val isModelReady: StateFlow<Boolean>
    fun summarize(text: String): Flow<String>
    fun abortGeneration()

    /** Suspends until no native generation is in progress (e.g. after [abortGeneration]). */
    suspend fun awaitGenerationIdle()
}
