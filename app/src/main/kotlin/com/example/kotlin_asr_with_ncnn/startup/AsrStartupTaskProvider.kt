package com.example.kotlin_asr_with_ncnn.startup

import android.app.Application
import com.example.kotlin_asr_with_ncnn.core.startup.StartupTaskProvider
import com.example.kotlin_asr_with_ncnn.startup.tasks.InitAsrModelTask
import com.example.kotlin_asr_with_ncnn.startup.tasks.InitLoggingTask
import com.example.kotlin_asr_with_ncnn.startup.tasks.ReadDisplayPreferencesTask
import com.example.kotlin_asr_with_ncnn.startup.tasks.WarmAppMetadataTask
import dagger.hilt.android.EntryPointAccessors

class AsrStartupTaskProvider(
    private val application: Application,
) : StartupTaskProvider {

    override fun provide() = EntryPointAccessors.fromApplication(
        application,
        StartupEntryPoint::class.java,
    ).let { ep ->
        listOf(
            InitLoggingTask(),
            WarmAppMetadataTask(application),
            ReadDisplayPreferencesTask(ep.themePreferences()),
            InitAsrModelTask(
                app = application,
                bridge = ep.ncnnNativeBridge(),
                notifier = ep.modelInitNotifier(),
            ),
        )
    }
}
