package com.hisabnikash.app.feature.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hisabnikash.app.data.preferences.UserPreferenceState
import com.hisabnikash.app.ui.theme.HisabTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `backup management explains separate deletion of external copies`() {
        // Removing the management entry or the distinction from local deletion must fail this test.
        compose.setContent {
            HisabTheme {
                SettingsScreen(
                    state = UserPreferenceState(guestWarningDismissed = true), onLanguageChange = {}, onDismissGuestWarning = {},
                    onDeleteAllData = {}, backupMessageRes = null, restoreDocumentUri = null,
                    onChooseBackupLocation = {}, onBackUpNow = {}, onChooseRestoreDocument = {},
                    onRestore = { _, _ -> }, onDismissRestore = {}, onDisableLocalBackup = {},
                    onConnectDrive = {}, onDriveBackUpNow = {}, onDriveRestoreLatest = {},
                    onDisconnectDrive = {}, showUndoRestore = false, onUndoRestore = {},
                    categories = emptyList(), onRenameCategory = { _, _ -> }, onArchiveCategory = {},
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        }
        compose.onNodeWithText("Manage or delete backup copies").performScrollTo().performClick()
        compose.onNodeWithText("Deleting local app data or disconnecting Drive does not delete existing backup copies.").assertExists()
        compose.onNodeWithText("Phone folder", substring = true).assertExists()
        compose.onNodeWithText("Manage apps", substring = true).assertExists()
        compose.onNodeWithText("Got it").performClick()
        compose.onNodeWithText("Deleting local app data or disconnecting Drive does not delete existing backup copies.").assertDoesNotExist()
    }
}
