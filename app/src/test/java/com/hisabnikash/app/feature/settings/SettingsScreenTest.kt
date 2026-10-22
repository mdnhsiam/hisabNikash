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

    private fun settings(
        state: UserPreferenceState,
        categories: List<com.hisabnikash.app.core.model.Category> = emptyList(),
        startAtBackup: Boolean = false,
        lateCategories: androidx.compose.runtime.State<List<com.hisabnikash.app.core.model.Category>>? = null,
    ) {
        compose.setContent { HisabTheme { SettingsScreen(
            state = state, onLanguageChange = {}, onDismissGuestWarning = {}, onDeleteAllData = {},
            backupMessageRes = null, restoreDocumentUri = null, onChooseBackupLocation = {}, onBackUpNow = {},
            onChooseRestoreDocument = {}, onRestore = { _, _ -> }, onDismissRestore = {}, onDisableLocalBackup = {},
            onConnectDrive = {}, onDriveBackUpNow = {}, onDriveRestoreLatest = {}, onDisconnectDrive = {},
            showUndoRestore = false, onUndoRestore = {}, categories = lateCategories?.value ?: categories,
            startAtBackup = startAtBackup,
            onRenameCategory = { _, _ -> false }, onArchiveCategory = {}, modifier = Modifier.verticalScroll(rememberScrollState()),
        ) } }
    }

    @Test fun `delayed category loading does not push backup shortcut controls offscreen`() {
        val categories = androidx.compose.runtime.mutableStateOf(emptyList<com.hisabnikash.app.core.model.Category>())
        settings(UserPreferenceState(guestWarningDismissed = true), startAtBackup = true, lateCategories = categories)
        compose.onNodeWithText("Choose backup folder").assertIsDisplayed()
        compose.runOnIdle {
            categories.value = (1..20).map { index ->
                com.hisabnikash.app.core.model.Category("custom$index", com.hisabnikash.app.core.model.CategoryType.EXPENSE,
                    "Custom $index", "Custom $index", index, false)
            }
        }
        compose.onNodeWithText("Choose backup folder").assertIsDisplayed()
    }

    @Test fun `failed category rename keeps the edited draft visible`() {
        val category = com.hisabnikash.app.core.model.Category("custom", com.hisabnikash.app.core.model.CategoryType.EXPENSE,
            "Travel", "Travel", 1, false)
        settings(UserPreferenceState(guestWarningDismissed = true), listOf(category))
        compose.onNodeWithText("Rename").performScrollTo().performClick()
        compose.onNodeWithText("Category").performTextReplacement("New travel name")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("New travel name").assertExists()
        compose.onNodeWithText("Rename category").assertExists()
    }

    @Test fun `folder selection without a snapshot is not shown as protected`() {
        settings(UserPreferenceState(localBackupEnabled = true, localBackupUri = "content://fixture"))
        compose.onNodeWithText("Encrypted phone-folder backup is enabled.").assertDoesNotExist()
        compose.onNodeWithText("Your data currently exists only inside this app.", substring = true).assertExists()
    }

    @Test fun `Drive connection for restore does not hide the unprotected data warning`() {
        settings(UserPreferenceState(driveBackupEnabled = true))
        compose.onNodeWithText("Your data currently exists only inside this app.", substring = true).assertExists()
    }

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
                    categories = emptyList(), onRenameCategory = { _, _ -> true }, onArchiveCategory = {},
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
