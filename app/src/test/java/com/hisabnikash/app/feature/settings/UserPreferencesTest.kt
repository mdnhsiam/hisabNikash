package com.hisabnikash.app.feature.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.data.preferences.AppLanguage
import com.hisabnikash.app.data.preferences.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UserPreferencesTest {
    @Test
    fun `preferences survive a new repository instance`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.preferencesDataStoreFile(UserPreferences.FILE_NAME).delete()
        val first = UserPreferences.create(context)
        first.setLanguage(AppLanguage.ENGLISH)
        first.setLastPaymentMethod(PaymentMethod.BKASH)
        first.dismissGuestWarning()
        first.setLocalBackup(enabled = true, uri = "content://hisab/backup")
        first.setDriveBackupEnabled(true)

        val restored = UserPreferences.create(context).state.first { it.driveBackupEnabled }

        assertEquals(AppLanguage.ENGLISH, restored.language)
        assertEquals(PaymentMethod.BKASH, restored.lastPaymentMethod)
        assertTrue(restored.guestWarningDismissed)
        assertTrue(restored.localBackupEnabled)
        assertEquals("content://hisab/backup", restored.localBackupUri)
        assertTrue(restored.driveBackupEnabled)
    }

    @Test
    fun `clearing backup state preserves language but resets backup and entry defaults`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.preferencesDataStoreFile(UserPreferences.FILE_NAME).delete()
        val preferences = UserPreferences.create(context)
        preferences.setLanguage(AppLanguage.ENGLISH)
        preferences.setLastPaymentMethod(PaymentMethod.NAGAD)
        preferences.dismissGuestWarning()
        preferences.setLocalBackup(true, "content://hisab/backup")
        preferences.setDriveBackupEnabled(true)

        preferences.clearPrivateState()
        val state = preferences.state.first { !it.driveBackupEnabled }

        assertEquals(AppLanguage.ENGLISH, state.language)
        assertEquals(PaymentMethod.CASH, state.lastPaymentMethod)
        assertFalse(state.guestWarningDismissed)
        assertFalse(state.localBackupEnabled)
        assertEquals(null, state.localBackupUri)
    }
}
