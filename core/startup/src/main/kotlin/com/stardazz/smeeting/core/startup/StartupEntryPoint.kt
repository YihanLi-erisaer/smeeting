package com.stardazz.smeeting.core.startup

import com.stardazz.smeeting.core.common.ThemePreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StartupEntryPoint {
    fun asrModelManager(): AsrModelManager
    fun llmModelManager(): LlmModelManager
    fun themePreferences(): ThemePreferences
    fun modelInitNotifier(): ModelInitNotifier
}