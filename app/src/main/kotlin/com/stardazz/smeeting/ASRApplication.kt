package com.stardazz.smeeting

import android.app.Application
import com.stardazz.smeeting.core.startup.TaskRegistry
import com.stardazz.smeeting.core.startup.AsrStartupTaskProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ASRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TaskRegistry.clear()
        TaskRegistry.register(AsrStartupTaskProvider(this))
    }
}