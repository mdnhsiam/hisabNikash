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
import com.hisabnikash.app.backup.drive.DriveAuthorizationGateway
import com.hisabnikash.app.backup.drive.DriveAuthorizationResult
import com.hisabnikash.app.backup.drive.DriveBackupStore
import com.hisabnikash.app.backup.RestoreRecoveryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.hisabnikash.app.data.repository.CategoryRepository
import com.hisabnikash.app.core.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

class SettingsViewModel(
    private val preferences: UserPreferences,
    private val dataDeletion: DataDeletionUseCase,
    private val localBackupStore: LocalBackupStore,
    private val driveAuthorization: DriveAuthorizationGateway,
    private val driveBackupStore: DriveBackupStore,
    @Suppress("UNUSED_PARAMETER") passwordCache: BackupPasswordCache,
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
    private var pendingAuthorizationVersion: Long? = null
    val categories: StateFlow<List<Category>> = categoryRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun launchOperation(failureMessage: Int = R.string.operation_failed, operation: suspend () -> Unit): Job = viewModelScope.launch {
        try { operation() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableBackupMessage.value = failureMessage }
    }

    private fun launchPasswordOperation(password: CharArray, failureMessage: Int, operation: suspend () -> Unit) {
        launchOperation(failureMessage) {
            try { operation() } finally { password.fill('\u0000') }
        }.invokeOnCompletion {
            // Covers cancellation before a cleared ViewModel's launch enters its body.
            password.fill('\u0000')
        }
    }

    fun setLanguage(language: AppLanguage, onApplied: () -> Unit) {
        launchOperation {
            preferences.setLanguage(language)
            onApplied()
        }
    }

    fun dismissGuestWarning() {
        launchOperation { preferences.dismissGuestWarning() }
    }

    fun deleteAllLocalData(onDeleted: () -> Unit = {}) {
        launchOperation {
            if (dataDeletion.deleteAllLocalData(confirmed = true)) onDeleted()
        }
    }

    fun chooseBackupLocation(uri: Uri) {
        launchOperation(R.string.backup_location_error) {
            localBackupStore.chooseBackupLocation(uri)
            mutableBackupMessage.value = R.string.backup_location_saved
        }
    }

    fun backUpNow(password: CharArray) {
        launchPasswordOperation(password, R.string.backup_failed) {
            withContext(Dispatchers.IO) { localBackupStore.backUpNow(password) }
            mutableBackupMessage.value = R.string.backup_success
        }
    }

    fun restore(uri: Uri, password: CharArray) {
        launchPasswordOperation(password, R.string.restore_failed) {
            val result = withContext(Dispatchers.IO) { localBackupStore.restoreFromDocument(uri, password) }
            mutableBackupMessage.value = if (result is RestoreResult.Success) R.string.restore_success else R.string.restore_failed
        }
    }

    fun disableLocalBackup() {
        launchOperation {
            localBackupStore.disable()
            mutableBackupMessage.value = R.string.local_backup_disabled
        }
    }

    fun authorizeDrive(activity: Activity) {
        val version = driveBackupStore.authorizationVersion
        launchOperation(R.string.drive_auth_failed) { handleDriveAuthorization(driveAuthorization.authorizeDriveAppData(activity), version) }
    }

    fun completeDriveAuthorization(data: Intent?) {
        val version = pendingAuthorizationVersion
        clearDriveResolution()
        if (data == null || version == null) {
            mutableBackupMessage.value = R.string.drive_auth_failed
            return
        }
        val result = runCatching { driveAuthorization.completeResolution(data) }.getOrElse(DriveAuthorizationResult::Failed)
        handleDriveAuthorization(result, version)
    }

    private fun clearDriveResolution() {
        pendingAuthorizationVersion = null
        mutableDriveResolution.value = null
    }

    private fun handleDriveAuthorization(result: DriveAuthorizationResult, version: Long) {
        if (version != driveBackupStore.authorizationVersion) {
            clearDriveResolution()
            mutableBackupMessage.value = R.string.drive_disconnected
            return
        }
        when (result) {
            is DriveAuthorizationResult.Authorized -> {
                clearDriveResolution()
                launchOperation(R.string.drive_auth_failed) {
                    mutableBackupMessage.value = if (driveBackupStore.connect(version)) R.string.drive_connected else R.string.drive_disconnected
                }
            }
            is DriveAuthorizationResult.ResolutionRequired -> {
                pendingAuthorizationVersion = version
                mutableDriveResolution.value = result.pendingIntent
            }
            is DriveAuthorizationResult.Failed -> {
                clearDriveResolution()
                mutableBackupMessage.value = R.string.drive_auth_failed
            }
        }
    }

    fun driveBackUpNow(password: CharArray) {
        launchPasswordOperation(password, R.string.drive_backup_failed) {
            // Silent authorization refreshes the token for every manual operation.
            // Any user-consent resolution is handled by the explicit Connect flow.
            val token = driveAuthorization.currentAccessToken()
            if (token == null) {
                mutableBackupMessage.value = R.string.drive_reconnect
            } else {
                withContext(Dispatchers.IO) { driveBackupStore.backUpNow(token, password) }
                mutableBackupMessage.value = R.string.drive_backup_success
            }
        }
    }

    fun driveRestoreLatest(password: CharArray) {
        launchPasswordOperation(password, R.string.restore_failed) {
            val token = driveAuthorization.currentAccessToken()
            if (token == null) mutableBackupMessage.value = R.string.drive_reconnect else {
                val result = withContext(Dispatchers.IO) { driveBackupStore.restoreLatest(token, password) }
                mutableBackupMessage.value = if (result is RestoreResult.Success) R.string.restore_success else R.string.restore_failed
            }
        }
    }

    fun disconnectDrive() {
        clearDriveResolution()
        launchOperation {
            driveBackupStore.disconnect { runCatching { driveAuthorization.revoke() }; Unit }
            mutableBackupMessage.value = R.string.drive_disconnected
        }
    }

    fun undoLastRestore(password: CharArray) {
        launchPasswordOperation(password, R.string.restore_failed) {
            val result = withContext(Dispatchers.IO) { recoveryStore.restore(password) }
            mutableBackupMessage.value = if (result is RestoreResult.Success) R.string.restore_undo_success else R.string.restore_failed
        }
    }

    fun hasRestoreRecovery(): Boolean = recoveryStore.exists()

    suspend fun renameCategory(category: Category, name: String): Boolean {
        if (name.isBlank() || category.isSystem) return false
        return try {
            categoryRepository.update(category.copy(nameEn = name.trim(), nameBn = name.trim()))
            true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableBackupMessage.value = R.string.operation_failed; false }
    }

    fun archiveCategory(category: Category) {
        if (!category.isSystem) launchOperation { categoryRepository.archive(category.id) }
    }
}
