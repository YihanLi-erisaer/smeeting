package com.example.kotlin_asr_with_ncnn.core.startup.tasks

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import com.example.kotlin_asr_with_ncnn.core.startup.StartupLogger
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTask
import com.example.kotlin_asr_with_ncnn.core.startup.AsrModelLoad
import com.example.kotlin_asr_with_ncnn.core.startup.ModelInitNotifier
import com.example.kotlin_asr_with_ncnn.core.startup.StartupPreferenceCache
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTaskIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InitAsrModelTask(
    private val app: Application,
    private val bridge: NcnnNativeBridge,
    private val notifier: ModelInitNotifier,
) : StartupTask {
    override val id: String = StartupTaskIds.ASR_MODEL
    override val dependencies: List<String> = listOf(StartupTaskIds.DISPLAY_PREFS)
    override val runOnMainThread: Boolean = false

    override suspend fun run() {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            StartupLogger.i(
                "InitAsrModelTask: skipped (no RECORD_AUDIO); will init after user grants in activity"
            )
            notifier.emitSkippedAwaitingPermission()
            return
        }

        val beam = StartupPreferenceCache.useBeamSearch
        StartupLogger.i("InitAsrModelTask: loading native model, useBeamSearch=$beam")

        val success = try {
            AsrModelLoad.load(bridge, app.assets, beam)
        } catch (e: Exception) {
            StartupLogger.e("InitAsrModelTask: exception", e)
            withContext(Dispatchers.Main) {
                notifier.emitFinished(false, e.message)
            }
            return
        }

        withContext(Dispatchers.Main) {
            if (success) {
                notifier.emitFinished(true, null)
            } else {
                notifier.emitFinished(false, "Model failed to load")
            }
        }
    }
}
