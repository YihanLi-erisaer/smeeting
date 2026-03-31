package com.example.kotlin_asr_with_ncnn.core.startup

import kotlinx.coroutines.launch

object StartupRunner {
    private val started = _root_ide_package_.java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * Runs registered startup DAG once per process, on a background dispatcher.
     * Call from a coroutine scope after the first frame (e.g. [androidx.compose.runtime.withFrameMillis]).
     */
    fun runRegisteredPipelineOnce(application: android.app.Application, scope: kotlinx.coroutines.CoroutineScope) {
        if (!started.compareAndSet(false, true)) {
            StartupLogger.d("StartupRunner: pipeline already executed, skipping")
            return
        }
        scope.launch(_root_ide_package_.kotlinx.coroutines.Dispatchers.Default) {
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