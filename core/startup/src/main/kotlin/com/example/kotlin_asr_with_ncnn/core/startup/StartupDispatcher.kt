package com.example.kotlin_asr_with_ncnn.core.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StartupDispatcher {

    fun dispatch(scope: CoroutineScope, task: StartupTask): Job {
        val dispatcher = if (task.runOnMainThread) {
            Dispatchers.Main.immediate
        } else {
            Dispatchers.Default
        }
        return scope.launch(dispatcher) {
            val thread = Thread.currentThread().name
            val startWall = System.currentTimeMillis()
            try {
                task.run()
            } finally {
                val endWall = System.currentTimeMillis()
                val durationMs = endWall - startWall
                StartupTracer.record(
                    id = task.id,
                    durationMs = durationMs,
                    threadName = thread,
                    startWallMs = startWall,
                    endWallMs = endWall,
                )
                StartupLogger.i(
                    "Startup task body done: id=${task.id} wall=${durationMs}ms thread=$thread"
                )
            }
        }
    }
}
