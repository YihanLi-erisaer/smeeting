package com.stardazz.smeeting.core.startup

import java.util.Collections
import kotlin.math.max
import kotlin.math.min

data class StartupTaskSpan(
    val id: String,
    val durationMs: Long,
    val startWallMs: Long,
    val endWallMs: Long,
    val threadName: String,
)

object StartupTracer {
    private val records = Collections.synchronizedList(mutableListOf<StartupTaskSpan>())
    @Volatile
    private var pipelineStartWallMs: Long = 0L

    fun markPipelineStart() {
        pipelineStartWallMs = System.currentTimeMillis()
    }

    fun clear() {
        synchronized(records) {
            records.clear()
        }
    }

    fun record(id: String, durationMs: Long, threadName: String, startWallMs: Long, endWallMs: Long) {
        records.add(
            StartupTaskSpan(
                id = id,
                durationMs = durationMs,
                startWallMs = startWallMs,
                endWallMs = endWallMs,
                threadName = threadName,
            )
        )
    }

    fun snapshot(): List<StartupTaskSpan> = synchronized(records) { records.toList() }

    private fun overlapMs(a: StartupTaskSpan, b: StartupTaskSpan): Long {
        val s = max(a.startWallMs, b.startWallMs)
        val e = min(a.endWallMs, b.endWallMs)
        return (e - s).coerceAtLeast(0)
    }

    /**
     * Prints per-task duration and pairs of tasks whose wall-clock windows overlapped (ran in parallel).
     */
    fun logParallelismReport(sink: (String) -> Unit) {
        val spans = snapshot().sortedBy { it.startWallMs }
        sink("===== Startup task timings (wall clock) =====")
        val base = if (pipelineStartWallMs > 0) pipelineStartWallMs else (spans.minOfOrNull { it.startWallMs } ?: 0L)
        spans.forEach { s ->
            val relStart = s.startWallMs - base
            sink(
                "  [${s.id}] ${s.durationMs}ms cpu/wait, " +
                    "t=${relStart}..${s.endWallMs - base}ms from pipeline start, thread=${s.threadName}"
            )
        }
        sink("===== Pairs with overlapping wall time (parallel work) =====")
        var any = false
        for (i in spans.indices) {
            for (j in i + 1 until spans.size) {
                val a = spans[i]
                val b = spans[j]
                val ov = overlapMs(a, b)
                if (ov > 0) {
                    any = true
                    sink("  ${a.id} || ${b.id}  (~${ov}ms overlap)")
                }
            }
        }
        if (!any) {
            sink("  (no overlap — tasks ran strictly one after another)")
        }
    }
}
