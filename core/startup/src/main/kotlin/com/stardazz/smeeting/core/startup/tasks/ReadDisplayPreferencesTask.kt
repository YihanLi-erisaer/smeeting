package com.stardazz.smeeting.core.startup.tasks

import com.stardazz.smeeting.core.common.ThemePreferences
import com.stardazz.smeeting.core.startup.StartupLogger
import com.stardazz.smeeting.core.startup.StartupTask
import com.stardazz.smeeting.core.startup.StartupPreferenceCache
import com.stardazz.smeeting.core.startup.StartupTaskIds
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
