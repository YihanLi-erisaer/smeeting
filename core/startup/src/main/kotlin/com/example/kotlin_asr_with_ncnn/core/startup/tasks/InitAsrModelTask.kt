package com.example.kotlin_asr_with_ncnn.core.startup.tasks

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.kotlin_asr_with_ncnn.core.startup.AsrModelManager
import com.example.kotlin_asr_with_ncnn.core.startup.ModelInitNotifier
import com.example.kotlin_asr_with_ncnn.core.startup.StartupLogger
import com.example.kotlin_asr_with_ncnn.core.startup.StartupPreferenceCache
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTask
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTaskIds

class InitAsrModelTask(
    private val app: Application,
    private val manager: AsrModelManager,
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
        manager.loadModel(app.assets, beam)
    }
}
