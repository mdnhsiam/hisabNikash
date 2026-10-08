package com.hisabnikash.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.data.preferences.AppLanguage
import com.hisabnikash.app.data.preferences.UserPreferenceState
import com.hisabnikash.app.core.model.Category

@Composable
fun SettingsScreen(
    state: UserPreferenceState,
    onLanguageChange: (AppLanguage) -> Unit,
    onDismissGuestWarning: () -> Unit,
    onDeleteAllData: () -> Unit,
    backupMessageRes: Int?,
    restoreDocumentUri: String?,
    onChooseBackupLocation: () -> Unit,
    onBackUpNow: (CharArray) -> Unit,
    onChooseRestoreDocument: () -> Unit,
    onRestore: (String, CharArray) -> Unit,
    onDismissRestore: () -> Unit,
    onDisableLocalBackup: () -> Unit,
    onConnectDrive: () -> Unit,
    onDriveBackUpNow: (CharArray) -> Unit,
    onDriveRestoreLatest: (CharArray) -> Unit,
    onDisconnectDrive: () -> Unit,
    showUndoRestore: Boolean,
    onUndoRestore: (CharArray) -> Unit,
    categories: List<Category>,
    onRenameCategory: (Category, String) -> Unit,
    onArchiveCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
    recurringContent: @Composable () -> Unit = {},
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showBackupPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var restorePassword by remember(restoreDocumentUri) { mutableStateOf("") }
    var drivePasswordAction by remember { mutableStateOf<DrivePasswordAction?>(null) }
    var drivePassword by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        LanguageOption(R.string.language_bangla, state.language == AppLanguage.BANGLA) {
            onLanguageChange(AppLanguage.BANGLA)
        }
        LanguageOption(R.string.language_english, state.language == AppLanguage.ENGLISH) {
            onLanguageChange(AppLanguage.ENGLISH)
        }

        val customCategories = categories.filterNot { it.isSystem }.filter { it.isActive }
        Text(stringResource(R.string.manage_categories), style = MaterialTheme.typography.titleMedium)
        if (customCategories.isEmpty()) Text(stringResource(R.string.no_custom_categories), color = MaterialTheme.colorScheme.onSurfaceVariant)
        customCategories.forEach { category ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category.nameBn.ifBlank { category.nameEn }, modifier = Modifier.padding(top = 12.dp))
                Row {
                    TextButton(onClick = { editingCategory = category }) { Text(stringResource(R.string.rename)) }
                    TextButton(onClick = { onArchiveCategory(category) }) { Text(stringResource(R.string.archive)) }
                }
            }
        }

        if (!state.localBackupEnabled && !state.driveBackupEnabled && !state.guestWarningDismissed) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.guest_data_warning), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onDismissGuestWarning) { Text(stringResource(R.string.got_it)) }
                }
            }
        }

        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.local_backup_risk), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.localBackupEnabled) {
            Text(stringResource(R.string.local_backup_enabled))
            state.lastLocalBackupAtEpochMillis?.let { Text(stringResource(R.string.last_backup_time, java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)))) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showBackupPassword = true }) { Text(stringResource(R.string.back_up_now)) }
                TextButton(onClick = onDisableLocalBackup) { Text(stringResource(R.string.disable_backup)) }
            }
        } else {
            Button(onClick = onChooseBackupLocation) { Text(stringResource(R.string.choose_backup_folder)) }
        }
        TextButton(onClick = onChooseRestoreDocument) { Text(stringResource(R.string.restore_from_backup)) }
        if (showUndoRestore) {
            TextButton(onClick = { drivePasswordAction = DrivePasswordAction.UNDO }) { Text(stringResource(R.string.undo_last_restore)) }
        }

        Text(stringResource(R.string.drive_backup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.drive_backup_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.driveBackupEnabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { drivePasswordAction = DrivePasswordAction.BACKUP }) {
                    Text(stringResource(R.string.drive_back_up_now))
                }
                TextButton(onClick = { drivePasswordAction = DrivePasswordAction.RESTORE }) {
                    Text(stringResource(R.string.drive_restore_latest))
                }
            }
            TextButton(onClick = onDisconnectDrive) { Text(stringResource(R.string.disconnect_drive)) }
        } else {
            Button(onClick = onConnectDrive) { Text(stringResource(R.string.connect_google_drive)) }
        }
        backupMessageRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.primary) }
        recurringContent()

        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.delete_all_data_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { showDeleteConfirmation = true }) { Text(stringResource(R.string.delete_all_data)) }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_all_data_question)) },
            text = { Text(stringResource(R.string.delete_all_data_warning)) },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirmation = false
                    onDeleteAllData()
                }) { Text(stringResource(R.string.delete_everything)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showBackupPassword) {
        PasswordDialog(
            titleRes = R.string.backup_password,
            password = password,
            onPasswordChange = { password = it },
            onConfirm = {
                onBackUpNow(password.toCharArray())
                password = ""
                showBackupPassword = false
            },
            onDismiss = { password = ""; showBackupPassword = false },
        )
    }

    restoreDocumentUri?.let { uri ->
        PasswordDialog(
            titleRes = R.string.restore_password,
            warningRes = R.string.restore_replacement_warning,
            password = restorePassword,
            onPasswordChange = { restorePassword = it },
            onConfirm = { onRestore(uri, restorePassword.toCharArray()); restorePassword = ""; onDismissRestore() },
            onDismiss = { restorePassword = ""; onDismissRestore() },
        )
    }

    drivePasswordAction?.let { action ->
        PasswordDialog(
            titleRes = if (action == DrivePasswordAction.BACKUP) R.string.backup_password else R.string.restore_password,
            warningRes = if (action == DrivePasswordAction.BACKUP) null else R.string.restore_replacement_warning,
            password = drivePassword,
            onPasswordChange = { drivePassword = it },
            onConfirm = {
                val value = drivePassword.toCharArray()
                when (action) {
                    DrivePasswordAction.BACKUP -> onDriveBackUpNow(value)
                    DrivePasswordAction.RESTORE -> onDriveRestoreLatest(value)
                    DrivePasswordAction.UNDO -> onUndoRestore(value)
                }
                drivePassword = ""
                drivePasswordAction = null
            },
            onDismiss = { drivePassword = ""; drivePasswordAction = null },
        )
    }

    editingCategory?.let { category ->
        var name by remember(category.id) { mutableStateOf(category.nameBn.ifBlank { category.nameEn }) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text(stringResource(R.string.rename_category)) },
            text = { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.category)) }) },
            confirmButton = { Button(onClick = { onRenameCategory(category, name); editingCategory = null }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private enum class DrivePasswordAction { BACKUP, RESTORE, UNDO }

@Composable
private fun LanguageOption(labelRes: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(stringResource(labelRes), modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun PasswordDialog(
    titleRes: Int,
    warningRes: Int? = null,
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    password,
                    onPasswordChange,
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Text(stringResource(R.string.password_warning), color = MaterialTheme.colorScheme.onSurfaceVariant)
                warningRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = password.length >= 8) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
