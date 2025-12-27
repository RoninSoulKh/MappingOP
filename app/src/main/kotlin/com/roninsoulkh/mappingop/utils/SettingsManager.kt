package com.roninsoulkh.mappingop.utils

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_THEME = "key_theme" // "Auto", "Light", "Dark"
    private const val KEY_LANG = "key_lang"   // "Ukr", "Eng"

    fun saveTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, "Auto") ?: "Auto"
    }

    fun saveLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang).apply()
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "Ukr") ?: "Ukr"
    }
}