package com.stardazz.smeeting.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prevents ASR and LLM inference from running simultaneously to avoid OOM on
 * mid-range devices. Both subsystems should acquire/release through this coordinator.
 */
@Singleton
class InferenceCoordinator @Inject constructor() {

    enum class ActiveEngine { NONE, ASR, LLM }

    private val mutex = Mutex()

    private val _activeEngine = MutableStateFlow(ActiveEngine.NONE)
    val activeEngine: StateFlow<ActiveEngine> = _activeEngine.asStateFlow()

    suspend fun acquireAsr(): Boolean = mutex.withLock {
        if (_activeEngine.value == ActiveEngine.LLM) return@withLock false
        _activeEngine.value = ActiveEngine.ASR
        true
    }

    suspend fun acquireLlm(): Boolean = mutex.withLock {
        if (_activeEngine.value == ActiveEngine.ASR) return@withLock false
        _activeEngine.value = ActiveEngine.LLM
        true
    }

    suspend fun release() = mutex.withLock {
        _activeEngine.value = ActiveEngine.NONE
    }
}
