package com.stardazz.smeeting.core.startup

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object StartupRunner {
    private val started = AtomicBoolean(false)

    /**
     * Runs registered startup DAG once per process, on a background dispatcher.
     * Call from a coroutine scope after the first frame (e.g. [androidx.compose.runtime.withFrameMillis]).
     */
    fun runRegisteredPipelineOnce(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) {
            StartupLogger.d("StartupRunner: pipeline already executed, skipping")
            return
        }
        scope.launch(Dispatchers.Default) {
            val tasks = TaskRegistry.collectTasks()
            if (tasks.isEmpty()) {
                StartupLogger.w("StartupRunner: no tasks in TaskRegistry")
                return@launch
            }
            StartupLogger.i("StartupRunner: beginning DAG (${tasks.size} tasks)")
            StartupManager(tasks).start(printDag = true) { StartupLogger.i(it) }
        }
    }
}