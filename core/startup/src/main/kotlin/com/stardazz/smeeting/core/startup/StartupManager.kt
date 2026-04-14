package com.stardazz.smeeting.core.startup

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class StartupManager(
    private val tasks: List<StartupTask>,
    private val dispatcher: StartupDispatcher = StartupDispatcher(),
) {

    /**
     * Runs the DAG: each task starts after [StartupTask.dependencies] complete.
     * Independent branches may run in parallel.
     */
    suspend fun start(
        printDag: Boolean = true,
        sink: (String) -> Unit = { StartupLogger.i(it) },
    ) {
        val totalMs = measureTimeMillis {
            coroutineScope {
                StartupTracer.clear()
                StartupTracer.markPipelineStart()
                val sorted = sortTasks(tasks)
                if (printDag) {
                    formatDagForLog(sorted).forEach { sink(it) }
                    val waves = parallelWavesForLog(tasks)
                    sink("===== Approx. parallel waves (same wave may run concurrently) =====")
                    waves.forEachIndexed { idx, ids ->
                        sink("  wave $idx: ${ids.joinToString(", ")}")
                    }
                }

                val jobById = LinkedHashMap<String, kotlinx.coroutines.Job>()
                sorted.forEach { task ->
                    val job = launch {
                        task.dependencies.forEach { depId ->
                            jobById[depId]?.join()
                        }
                        dispatcher.dispatch(this@coroutineScope, task).join()
                    }
                    jobById[task.id] = job
                }

                jobById.values.joinAll()
            }
        }
        sink("===== StartupManager total wall time: ${totalMs}ms =====")
        StartupTracer.logParallelismReport(sink)
    }
}
