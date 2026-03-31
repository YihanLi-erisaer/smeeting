package com.example.kotlin_asr_with_ncnn.core.startup.tasks

import com.example.kotlin_asr_with_ncnn.core.common.ThemePreferences
import com.example.kotlin_asr_with_ncnn.core.startup.StartupLogger
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTask
import com.example.kotlin_asr_with_ncnn.core.startup.StartupPreferenceCache
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTaskIds
import kotlinx.coroutines.flow.first

/**
 * First read of DataStore for beam search etc. [InitAsrModelTask] depends on this id.
 */
class ReadDisplayPreferencesTask(
    private val themePreferences: ThemePreferences,
) : StartupTask {
    override val id: String = StartupTaskIds.DISPLAY_PREFS
    override val dependencies: List<String> = emptyList()
    override val runOnMainThread: Boolean = false
    override val priority: Int = 7

    override suspend fun run() {
        val beam = themePreferences.useBeamSearchFlow.first()
        StartupPreferenceCache.useBeamSearch = beam
        StartupLogger.i(
            "ReadDisplayPreferencesTask: useBeamSearch=$beam (parallel with logging / app meta)"
        )
    }
}
