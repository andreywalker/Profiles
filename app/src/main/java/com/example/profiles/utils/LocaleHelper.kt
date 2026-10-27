package com.example.profiles.utils
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {
    private val systemLocale: Locale = Resources.getSystem().configuration.locales[0]

    /** lang = "system", "en" or "ru" */
    fun wrap(context: Context, lang: String): Context {
        if (lang == "system") {
            Locale.setDefault(systemLocale)
            return context
        }
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)              // also makes dates format in this language
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}