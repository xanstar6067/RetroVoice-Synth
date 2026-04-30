package com.example.retrovoicesynth.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class ThemePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun applySavedMode() {
        AppCompatDelegate.setDefaultNightMode(currentMode())
    }

    fun toggleMode(): Int {
        val nextMode = if (isDarkMode()) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        preferences.edit().putInt(KEY_THEME_MODE, nextMode).apply()
        AppCompatDelegate.setDefaultNightMode(nextMode)
        return nextMode
    }

    fun isDarkMode(): Boolean = currentMode() == AppCompatDelegate.MODE_NIGHT_YES

    private fun currentMode(): Int {
        return preferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_YES)
    }

    private companion object {
        const val PREFERENCES_NAME = "retro_voice_theme"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
