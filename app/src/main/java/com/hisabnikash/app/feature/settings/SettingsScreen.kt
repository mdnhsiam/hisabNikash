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

@Composable
fun SettingsScreen(
    state: UserPreferenceState,
    onLanguageChange: (AppLanguage) -> Unit,
    onDismissGuestWarning: () -> Unit,
    onDeleteAllData: () -> Unit,
    modifier: Modifier = Modifier,
    recurringContent: @Composable () -> Unit = {},
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        LanguageOption(R.string.language_bangla, state.language == AppLanguage.BANGLA) {
            onLanguageChange(AppLanguage.BANGLA)
        }
        LanguageOption(R.string.language_english, state.language == AppLanguage.ENGLISH) {
            onLanguageChange(AppLanguage.ENGLISH)
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
        Text(stringResource(R.string.backup_setup_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun LanguageOption(labelRes: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(stringResource(labelRes), modifier = Modifier.padding(top = 12.dp))
    }
}
