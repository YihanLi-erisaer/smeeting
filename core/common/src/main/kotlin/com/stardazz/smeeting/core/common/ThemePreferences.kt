package com.stardazz.smeeting.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class ThemeMode { FOLLOW_SYSTEM, LIGHT, DARK }

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
private val KEY_USE_BEAM_SEARCH = booleanPreferencesKey("use_beam_search")

class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val themeModeFlow: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences ->
            when (preferences[KEY_THEME_MODE]) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.FOLLOW_SYSTEM
            }
        }

    /** When false (default), greedy search; when true, modified beam search. */
    val useBeamSearchFlow: Flow<Boolean> = context.themeDataStore.data
        .map { preferences -> preferences[KEY_USE_BEAM_SEARCH] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = when (mode) {
                ThemeMode.FOLLOW_SYSTEM -> "system"
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
            }
        }
    }

    suspend fun setUseBeamSearch(value: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_USE_BEAM_SEARCH] = value
        }
    }
}
