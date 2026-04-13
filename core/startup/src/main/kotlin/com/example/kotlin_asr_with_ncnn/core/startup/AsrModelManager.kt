package com.example.kotlin_asr_with_ncnn.core.startup

import android.content.res.AssetManager
import android.util.Log
import com.example.kotlin_asr_with_ncnn.core.media.ModelConfig
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for ASR model lifecycle (load / reload / release).
 *
 * Both the startup DAG ([tasks.InitAsrModelTask]) and runtime triggers
 * ([com.example.kotlin_asr_with_ncnn.AsrModelCoordinator]) go through this class,
 * so concurrent calls are serialized by [mutex] and notification is consistent.
 */
@Singleton
class AsrModelManager @Inject constructor(
    private val nativeBridge: NcnnNativeBridge,
    private val notifier: ModelInitNotifier,
) {
    private val mutex = Mutex()

    /**
     * Load the model (Vulkan first, CPU fallback).
     * Safe to call from any dispatcher; heavy work runs on [Dispatchers.Default].
     */
    suspend fun loadModel(assets: AssetManager, useBeamSearch: Boolean) {
        mutex.withLock {
            loadInternal(assets, useBeamSearch)
        }
    }

    /**
     * Release the current model and reload with (possibly different) settings.
     * Used when the user toggles beam-search at runtime.
     */
    suspend fun reloadModel(assets: AssetManager, useBeamSearch: Boolean) {
        mutex.withLock {
            nativeBridge.releaseModel()
            loadInternal(assets, useBeamSearch)
        }
    }

    private suspend fun loadInternal(assets: AssetManager, useBeamSearch: Boolean) {
        try {
            val success = doLoad(assets, useBeamSearch)
            notifier.emitFinished(success, if (success) null else "Model failed to load")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during ASR model initialization", e)
            notifier.emitFinished(false, e.message)
        }
    }

    private suspend fun doLoad(
        assets: AssetManager,
        useBeamSearch: Boolean,
    ): Boolean = withContext(Dispatchers.Default) {
        fun config(useVulkan: Boolean) = ModelConfig(
            encoderParam = "encoder.param",
            encoderBin = "encoder.bin",
            decoderParam = "decoder.param",
            decoderBin = "decoder.bin",
            joinerParam = "joiner.param",
            joinerBin = "joiner.bin",
            tokens = "tokens.txt",
            numThreads = 4,
            useVulkanCompute = useVulkan,
            useBeamSearch = useBeamSearch,
        )

        if (nativeBridge.initModel(assets, config(useVulkan = true))) {
            return@withContext true
        }

        Log.i(TAG, "Vulkan init failed or unavailable; retrying with CPU only")
        nativeBridge.releaseModel()
        nativeBridge.initModel(assets, config(useVulkan = false))
    }

    private companion object {
        const val TAG = "AsrModelManager"
    }
}
