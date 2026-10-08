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
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.hisabnikash.app.backup.crypto.BackupPasswordCache
import com.hisabnikash.app.backup.drive.DriveAuthorizationClient
import com.hisabnikash.app.backup.drive.DriveAuthorizationResult
import com.hisabnikash.app.backup.drive.DriveBackupStore
import com.hisabnikash.app.backup.RestoreRecoveryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.hisabnikash.app.data.repository.CategoryRepository
import com.hisabnikash.app.core.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val preferences: UserPreferences,
    private val dataDeletion: DataDeletionUseCase,
    private val localBackupStore: LocalBackupStore,
    private val driveAuthorization: DriveAuthorizationClient,
    private val driveBackupStore: DriveBackupStore,
    private val passwordCache: BackupPasswordCache,
    private val recoveryStore: RestoreRecoveryStore,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    val state: StateFlow<UserPreferenceState> = preferences.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferenceState(),
    )
    private val mutableBackupMessage = MutableStateFlow<Int?>(null)
    val backupMessage = mutableBackupMessage.asStateFlow()
    private val mutableDriveResolution = MutableStateFlow<PendingIntent?>(null)
    val driveResolution = mutableDriveResolution.asStateFlow()
    private var driveAccessToken: String? = null
    val categories: StateFlow<List<Category>> = categoryRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            mutableBackupMessage.value = runCatching { withContext(Dispatchers.IO) { localBackupStore.backUpNow(password) } }
                .fold({ R.string.backup_success }, { R.string.backup_failed })
            password.fill('\u0000')
        }
    }

    fun restore(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            mutableBackupMessage.value = runCatching { withContext(Dispatchers.IO) { localBackupStore.restoreFromDocument(uri, password) } }
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

    fun authorizeDrive(activity: Activity) {
        viewModelScope.launch { handleDriveAuthorization(driveAuthorization.authorizeDriveAppData(activity)) }
    }

    fun completeDriveAuthorization(data: Intent?) {
        if (data == null) {
            mutableBackupMessage.value = R.string.drive_auth_failed
            return
        }
        handleDriveAuthorization(driveAuthorization.completeResolution(data))
    }

    private fun handleDriveAuthorization(result: DriveAuthorizationResult) {
        when (result) {
            is DriveAuthorizationResult.Authorized -> {
                driveAccessToken = result.accessToken
                mutableDriveResolution.value = null
                viewModelScope.launch { preferences.setDriveBackupEnabled(true) }
                mutableBackupMessage.value = R.string.drive_connected
            }
            is DriveAuthorizationResult.ResolutionRequired -> mutableDriveResolution.value = result.pendingIntent
            is DriveAuthorizationResult.Failed -> mutableBackupMessage.value = R.string.drive_auth_failed
        }
    }

    fun driveBackUpNow(password: CharArray) {
        viewModelScope.launch {
            val token = driveAccessToken ?: driveAuthorization.currentAccessToken()
            mutableBackupMessage.value = if (token == null) {
                R.string.drive_reconnect
            } else {
                runCatching {
                    withContext(Dispatchers.IO) { driveBackupStore.backUpNow(token, password) }
                    passwordCache.save(password)
                }.fold({ R.string.drive_backup_success }, { R.string.drive_backup_failed })
            }
            password.fill('\u0000')
        }
    }

    fun driveRestoreLatest(password: CharArray) {
        viewModelScope.launch {
            val token = driveAccessToken ?: driveAuthorization.currentAccessToken()
            mutableBackupMessage.value = if (token == null) R.string.drive_reconnect else runCatching {
                withContext(Dispatchers.IO) { driveBackupStore.restoreLatest(token, password) }
            }.fold(
                { result -> if (result is RestoreResult.Success) R.string.restore_success else R.string.restore_failed },
                { R.string.restore_failed },
            )
            password.fill('\u0000')
        }
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            runCatching { driveAuthorization.revoke() }
            driveAccessToken = null
            preferences.setDriveBackupEnabled(false)
            mutableBackupMessage.value = R.string.drive_disconnected
        }
    }

    fun undoLastRestore(password: CharArray) {
        viewModelScope.launch {
            mutableBackupMessage.value = runCatching { recoveryStore.restore(password) }.fold(
                { result -> if (result is RestoreResult.Success) R.string.restore_undo_success else R.string.restore_failed },
                { R.string.restore_failed },
            )
            password.fill('\u0000')
        }
    }

    fun hasRestoreRecovery(): Boolean = recoveryStore.exists()

    fun renameCategory(category: Category, name: String) {
        if (name.isBlank() || category.isSystem) return
        viewModelScope.launch { categoryRepository.update(category.copy(nameEn = name.trim(), nameBn = name.trim())) }
    }

    fun archiveCategory(category: Category) {
        if (!category.isSystem) viewModelScope.launch { categoryRepository.archive(category.id) }
    }
}
