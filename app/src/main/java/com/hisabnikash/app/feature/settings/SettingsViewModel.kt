package com.hisabnikash.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.data.preferences.AppLanguage
import com.hisabnikash.app.data.preferences.UserPreferenceState
import com.hisabnikash.app.data.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import com.hisabnikash.app.R
import com.hisabnikash.app.backup.RestoreResult
import com.hisabnikash.app.backup.local.LocalBackupStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val preferences: UserPreferences,
    private val dataDeletion: DataDeletionUseCase,
    private val localBackupStore: LocalBackupStore,
) : ViewModel() {
    val state: StateFlow<UserPreferenceState> = preferences.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferenceState(),
    )
    private val mutableBackupMessage = MutableStateFlow<Int?>(null)
    val backupMessage = mutableBackupMessage.asStateFlow()

    fun setLanguage(language: AppLanguage, onApplied: () -> Unit) {
        viewModelScope.launch {
            preferences.setLanguage(language)
            onApplied()
        }
    }

    fun dismissGuestWarning() {
        viewModelScope.launch { preferences.dismissGuestWarning() }
    }

    fun deleteAllLocalData(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            if (dataDeletion.deleteAllLocalData(confirmed = true)) onDeleted()
        }
    }

    fun chooseBackupLocation(uri: Uri) {
        viewModelScope.launch {
            mutableBackupMessage.value = runCatching { localBackupStore.chooseBackupLocation(uri) }
                .fold({ R.string.backup_location_saved }, { R.string.backup_location_error })
        }
    }

    fun backUpNow(password: CharArray) {
        viewModelScope.launch {
            mutableBackupMessage.value = runCatching { localBackupStore.backUpNow(password) }
                .fold({ R.string.backup_success }, { R.string.backup_failed })
            password.fill('\u0000')
        }
    }

    fun restore(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            mutableBackupMessage.value = runCatching { localBackupStore.restoreFromDocument(uri, password) }
                .fold(
                    { result -> if (result is RestoreResult.Success) R.string.restore_success else R.string.restore_failed },
                    { R.string.restore_failed },
                )
            password.fill('\u0000')
        }
    }

    fun disableLocalBackup() {
        viewModelScope.launch {
            localBackupStore.disable()
            mutableBackupMessage.value = R.string.local_backup_disabled
        }
    }
}
