package com.example.kotlin_asr_with_ncnn

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "theme_prefs"
private const val KEY_DARK_THEME = "dark_theme"

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()
}
