package com.example.securecredential.core.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Settings > Language. Korean is the app's own default regardless of device system locale. */
enum class AppLanguage(val languageTag: String) {
    KOREAN("ko"),
    ENGLISH("en");

    companion object {
        val DEFAULT = KOREAN

        fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return entries.find { tag.startsWith(it.languageTag) } ?: DEFAULT
        }

        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.languageTag))
        }

        /** Call once at app startup — only sets a default if the user hasn't chosen one yet. */
        fun applyDefaultIfUnset() {
            if (AppCompatDelegate.getApplicationLocales().isEmpty) {
                apply(DEFAULT)
            }
        }
    }
}
