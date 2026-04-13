package com.example.kotlin_asr_with_ncnn

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.kotlin_asr_with_ncnn.core.common.ThemePreferences
import com.example.kotlin_asr_with_ncnn.core.startup.AsrModelManager
import com.example.kotlin_asr_with_ncnn.core.startup.ModelInitNotifier
import com.example.kotlin_asr_with_ncnn.core.startup.ModelInitPipelineEvent
import com.example.kotlin_asr_with_ncnn.core.startup.StartupPreferenceCache
import com.example.kotlin_asr_with_ncnn.core.startup.StartupRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AsrModelCoordinator(
    private val activity: ComponentActivity,
    private val asrModelManager: AsrModelManager,
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
                StartupPreferenceCache.useBeamSearch = useBeamSearch
                asrModelManager.loadModel(activity.assets, useBeamSearch)
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
            asrModelManager.reloadModel(activity.assets, useBeamSearch)
        }
    }

    private companion object {
        const val TAG = "AsrModelCoordinator"
    }
}
