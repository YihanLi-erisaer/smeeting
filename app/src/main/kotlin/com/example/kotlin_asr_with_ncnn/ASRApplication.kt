package com.example.kotlin_asr_with_ncnn

import android.app.Application
import com.example.kotlin_asr_with_ncnn.core.startup.TaskRegistry
import com.example.kotlin_asr_with_ncnn.core.startup.AsrStartupTaskProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ASRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TaskRegistry.clear()
        TaskRegistry.register(AsrStartupTaskProvider(this))
    }
}