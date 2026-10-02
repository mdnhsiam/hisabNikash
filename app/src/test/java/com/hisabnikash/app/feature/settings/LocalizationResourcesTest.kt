package com.hisabnikash.app.feature.settings

import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.R
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalizationResourcesTest {
    @Test
    fun `every user-visible resource has a Bangla translation`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val english = base.forLanguage("en")
        val bangla = base.forLanguage("bn")
        val localeIndependent = setOf("payment_bkash", "payment_nagad")

        R.string::class.java.fields.forEach { field ->
            val id = field.getInt(null)
            val en = english.getString(id)
            val bn = bangla.getString(id)
            assertFalse("${field.name} has a blank Bangla value", bn.isBlank())
            if (field.name !in localeIndependent) {
                assertNotEquals("${field.name} fell back to English", en, bn)
            }
        }
    }

    private fun Context.forLanguage(tag: String): Context {
        val configuration = resources.configuration
        configuration.setLocales(LocaleList(Locale.forLanguageTag(tag)))
        return createConfigurationContext(configuration)
    }
}
