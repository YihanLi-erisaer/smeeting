package com.example.kotlin_asr_with_ncnn

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.kotlin_asr_with_ncnn.core.common.ThemePreferences
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import com.example.kotlin_asr_with_ncnn.core.startup.AsrModelLoad
import com.example.kotlin_asr_with_ncnn.core.startup.ModelInitNotifier
import com.example.kotlin_asr_with_ncnn.core.startup.ModelInitPipelineEvent
import com.example.kotlin_asr_with_ncnn.core.startup.StartupPreferenceCache
import com.example.kotlin_asr_with_ncnn.core.startup.StartupRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AsrModelCoordinator(
    private val activity: ComponentActivity,
    private val nativeBridge: NcnnNativeBridge,
    private val themePreferences: ThemePreferences,
    private val modelInitNotifier: ModelInitNotifier,
    private val mainUiViewModel: MainUiViewModel,
) {
    private var syncedDecoderMode: Boolean? = null

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onAudioPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            activity.lifecycleScope.launch {
                val useBeamSearch = themePreferences.useBeamSearchFlow.first()
                initAsrModel(useBeamSearch)
            }
        } else {
            Log.e(TAG, "Audio recording permission denied")
            mainUiViewModel.setModelInitResult(
                success = false,
                errorMessage = "Microphone permission required",
            )
        }
    }

    suspend fun observeModelInitEvents() {
        modelInitNotifier.events.collect { event ->
            when (event) {
                is ModelInitPipelineEvent.Finished -> {
                    mainUiViewModel.setModelInitResult(
                        success = event.success,
                        errorMessage = event.error,
                    )
                }
                ModelInitPipelineEvent.SkippedAwaitingPermission -> {
                    Log.d(
                        TAG,
                        "Startup pipeline deferred ASR init until RECORD_AUDIO is granted",
                    )
                }
            }
        }
    }

    fun runStartupPipeline(scope: CoroutineScope) {
        StartupRunner.runRegisteredPipelineOnce(scope)
    }

    suspend fun syncDecoderMode(useBeamSearch: Boolean, modelInitState: ModelInitState) {
        if (modelInitState !is ModelInitState.Ready) return

        val synced = syncedDecoderMode
        if (synced == null) {
            syncedDecoderMode = useBeamSearch
            return
        }
        if (synced == useBeamSearch) return

        syncedDecoderMode = useBeamSearch
        if (hasAudioPermission()) {
            nativeBridge.releaseModel()
            initAsrModel(useBeamSearch)
        }
    }

    private fun initAsrModel(useBeamSearch: Boolean) {
        activity.lifecycleScope.launch(Dispatchers.Default) {
            try {
                StartupPreferenceCache.useBeamSearch = useBeamSearch
                val success = AsrModelLoad.load(nativeBridge, activity.assets, useBeamSearch)
                withContext(Dispatchers.Main) {
                    modelInitNotifier.emitFinished(
                        success = success,
                        error = if (success) null else "Model failed to load",
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during ASR initialization", e)
                withContext(Dispatchers.Main) {
                    modelInitNotifier.emitFinished(false, e.message)
                }
            }
        }
    }

    private companion object {
        const val TAG = "AsrModelCoordinator"
    }
}
