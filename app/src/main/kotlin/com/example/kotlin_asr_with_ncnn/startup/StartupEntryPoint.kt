package com.example.kotlin_asr_with_ncnn.startup

import com.example.kotlin_asr_with_ncnn.ThemePreferences
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StartupEntryPoint {
    fun ncnnNativeBridge(): NcnnNativeBridge
    fun themePreferences(): ThemePreferences
    fun modelInitNotifier(): ModelInitNotifier
}
