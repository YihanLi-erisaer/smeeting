package com.stardazz.smeeting.core.startup.tasks

import com.stardazz.smeeting.core.startup.StartupLogger
import com.stardazz.smeeting.core.startup.StartupTask
import com.stardazz.smeeting.core.startup.StartupTaskIds
import kotlinx.coroutines.delay

/**
 * Lightweight task with no deps — runs in parallel with other root tasks.
 */
class InitLoggingTask : StartupTask {
    override val id: String = StartupTaskIds.INIT_LOGGING
    override val dependencies: List<String> = emptyList()
    override val runOnMainThread: Boolean = false
    override val priority: Int = 10

    override suspend fun run() {
        StartupLogger.i("InitLoggingTask: diagnostics channel ready")
        delay(5)
    }
}
