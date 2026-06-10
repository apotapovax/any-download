package com.alexp.anydownload.ui.theme

import android.content.Context

object ThemePreferences {
    private const val PREFS = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean? {
        if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_DARK_MODE)) {
            return null
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }
}
