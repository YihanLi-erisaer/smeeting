package com.stardazz.smeeting.core.startup.tasks

import android.app.Application
import com.stardazz.smeeting.core.startup.LlmModelManager
import com.stardazz.smeeting.core.startup.StartupLogger
import com.stardazz.smeeting.core.startup.StartupTask
import com.stardazz.smeeting.core.startup.StartupTaskIds

class InitLlmModelTask(
    private val app: Application,
    private val manager: LlmModelManager,
) : StartupTask {
    override val id: String = StartupTaskIds.LLM_MODEL
    override val dependencies: List<String> = listOf(StartupTaskIds.INIT_LOGGING)
    override val runOnMainThread: Boolean = false

    override suspend fun run() {
        if (manager.isModelDownloaded(app)) {
            StartupLogger.i("InitLlmModelTask: model present on disk, loading")
            manager.loadModel(app, nThreads = 4)
        } else {
            StartupLogger.i("InitLlmModelTask: model not yet downloaded, skipping auto-load")
        }
    }
}
