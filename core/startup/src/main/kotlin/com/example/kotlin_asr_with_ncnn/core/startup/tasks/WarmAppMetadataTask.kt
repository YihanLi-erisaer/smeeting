package com.example.kotlin_asr_with_ncnn.core.startup.tasks

import android.app.Application
import com.example.kotlin_asr_with_ncnn.core.startup.StartupLogger
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTask
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTaskIds

/**
 * Reads app metadata unrelated to ASR — parallel with prefs + logging roots.
 */
class WarmAppMetadataTask(
    private val app: Application,
) : StartupTask {
    override val id: String = StartupTaskIds.WARM_APP_META
    override val dependencies: List<String> = emptyList()
    override val runOnMainThread: Boolean = false
    override val priority: Int = 5

    override suspend fun run() {
        @Suppress("DEPRECATION")
        val version = try {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }
        StartupLogger.i("WarmAppMetadataTask: versionName=$version (non-ASR warm-up)")
    }
}
