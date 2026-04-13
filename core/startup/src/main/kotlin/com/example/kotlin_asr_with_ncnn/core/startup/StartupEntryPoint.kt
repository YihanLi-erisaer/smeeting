package com.example.kotlin_asr_with_ncnn.core.startup

import com.example.kotlin_asr_with_ncnn.core.common.ThemePreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StartupEntryPoint {
    fun asrModelManager(): AsrModelManager
    fun themePreferences(): ThemePreferences
    fun modelInitNotifier(): ModelInitNotifier
}