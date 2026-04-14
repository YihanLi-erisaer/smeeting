package com.stardazz.smeeting.core.startup

/**
 * Filled by [ReadDisplayPreferencesTask] before [InitAsrModelTask] runs (DAG dependency).
 */
object StartupPreferenceCache {
    @Volatile
    var useBeamSearch: Boolean = false
}