package com.stardazz.smeeting.core.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NcnnLlmBridge @Inject constructor() {

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val isGeneratingAtomic = AtomicBoolean(false)

    private val _isGeneratingFlow = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGeneratingFlow.asStateFlow()

    private val _tokenFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    companion object {
        private const val TAG = "NcnnLlmBridge"

        init {
            try {
                System.loadLibrary("llm_inference")
                Log.i(TAG, "Native library llm_inference loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
                throw e
            }
        }
    }

    @Suppress("unused")
    fun onTokenGenerated(token: String) {
        _tokenFlow.tryEmit(token)
    }

    suspend fun loadModel(
        modelPath: String,
        useVulkan: Boolean,
        nThreads: Int = 4,
        vulkanDeviceIndex: Int = 0,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val success = loadModelNative(modelPath, useVulkan, nThreads, vulkanDeviceIndex)
            _isLoaded.value = success
            if (success) {
                Log.i(TAG, "Model loaded from $modelPath (vulkan=$useVulkan)")
            } else {
                Log.e(TAG, "Failed to load model from $modelPath")
            }
            success
        }

    fun releaseModel() {
        releaseModelNative()
        _isLoaded.value = false
    }

    suspend fun generate(prompt: String, maxTokens: Int = 512, nThreads: Int = 4): String {
        if (!_isLoaded.value) return ""
        if (!isGeneratingAtomic.compareAndSet(false, true)) {
            Log.w(TAG, "Generation already in progress")
            return ""
        }
        _isGeneratingFlow.value = true
        return try {
            withContext(Dispatchers.IO) {
                generateNative(prompt, maxTokens, nThreads)
            }
        } finally {
            isGeneratingAtomic.set(false)
            _isGeneratingFlow.value = false
        }
    }

    fun abort() {
        abortNative()
    }

    fun tokenFlow(): Flow<String> = _tokenFlow

    private external fun loadModelNative(
        modelPath: String,
        useVulkan: Boolean,
        nThreads: Int,
        vulkanDeviceIndex: Int,
    ): Boolean

    private external fun releaseModelNative()
    private external fun abortNative()
    private external fun generateNative(prompt: String, maxTokens: Int, nThreads: Int): String
    private external fun isModelLoadedNative(): Boolean
}
