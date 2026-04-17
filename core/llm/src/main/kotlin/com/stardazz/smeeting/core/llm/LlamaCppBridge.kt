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
class LlamaCppBridge @Inject constructor() {

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val isGeneratingAtomic = AtomicBoolean(false)

    /** True while [generateNative] is running; used to await cancel completion. */
    private val _isGeneratingFlow = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGeneratingFlow.asStateFlow()

    private val _tokenFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    companion object {
        private const val TAG = "LlamaCppBridge"

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

    @Suppress("unused") // Called from JNI
    fun onTokenGenerated(token: String) {
        _tokenFlow.tryEmit(token)
    }

    suspend fun loadModel(modelPath: String, nThreads: Int = 4): Boolean =
        withContext(Dispatchers.IO) {
            val success = loadModelNative(modelPath, nThreads)
            _isLoaded.value = success
            if (success) {
                Log.i(TAG, "Model loaded from $modelPath")
            } else {
                Log.e(TAG, "Failed to load model from $modelPath")
            }
            success
        }

    fun releaseModel() {
        releaseModelNative()
        _isLoaded.value = false
    }

    /**
     * Runs generation on [Dispatchers.IO]. Returns the full generated text.
     * Tokens are streamed via [tokenFlow] as they are produced.
     */
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

    private external fun loadModelNative(modelPath: String, nThreads: Int): Boolean
    private external fun releaseModelNative()
    private external fun abortNative()
    private external fun generateNative(prompt: String, maxTokens: Int, nThreads: Int): String
    private external fun isModelLoadedNative(): Boolean
}
