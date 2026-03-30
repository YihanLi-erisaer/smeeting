package com.example.kotlin_asr_with_ncnn.core.startup

/**
 * One unit of startup work. Collected via [StartupTaskProvider], ordered by [StartupManager]
 * using [dependencies] as a DAG.
 */
interface StartupTask {
    val id: String
    val dependencies: List<String>
    val runOnMainThread: Boolean

    /**
     * Higher runs earlier among tasks that are ready at the same time (tie-break after topo sort).
     */
    val priority: Int get() = 0

    suspend fun run()
}
