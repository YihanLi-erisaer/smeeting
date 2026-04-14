package com.stardazz.smeeting.core.startup

import android.app.Application
import com.stardazz.smeeting.core.startup.tasks.InitAsrModelTask
import com.stardazz.smeeting.core.startup.tasks.InitLoggingTask
import com.stardazz.smeeting.core.startup.tasks.ReadDisplayPreferencesTask
import com.stardazz.smeeting.core.startup.tasks.WarmAppMetadataTask
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
                manager = ep.asrModelManager(),
                notifier = ep.modelInitNotifier(),
            ),
        )
    }
}