package com.hisabnikash.app.feature.settings

import android.content.Context
import android.content.res.Configuration
import com.hisabnikash.app.data.preferences.AppLanguage
import java.util.Locale

object LanguageController {
    fun localizedContext(context: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
