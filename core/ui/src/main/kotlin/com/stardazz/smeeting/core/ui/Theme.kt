package com.stardazz.smeeting.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.stardazz.smeeting.core.common.ThemeMode

@Stable
class ThemeState internal constructor(
    initialThemeMode: ThemeMode,
    private val onThemeModeChanged: (ThemeMode) -> Unit,
) {
    var themeMode by mutableStateOf(initialThemeMode)
        private set

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        onThemeModeChanged(mode)
    }
}

@Composable
fun rememberThemeState(
    initialThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
): ThemeState {
    return remember(initialThemeMode, onThemeModeChanged) {
        ThemeState(initialThemeMode = initialThemeMode, onThemeModeChanged = onThemeModeChanged)
    }
}
