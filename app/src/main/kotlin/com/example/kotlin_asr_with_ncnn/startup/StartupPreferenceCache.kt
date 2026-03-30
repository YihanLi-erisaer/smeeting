package com.example.kotlin_asr_with_ncnn.startup

/**
 * Filled by [ReadDisplayPreferencesTask] before [InitAsrModelTask] runs (DAG dependency).
 */
object StartupPreferenceCache {
    @Volatile
    var useBeamSearch: Boolean = false
}
