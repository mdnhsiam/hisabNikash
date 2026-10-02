package com.hisabnikash.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.data.repository.PaymentMethodPreference
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface LocalBackupPreferences {
    val state: Flow<UserPreferenceState>
    suspend fun setLocalBackup(enabled: Boolean, uri: String?)
    suspend fun setLastLocalBackupAt(epochMillis: Long?)
}

enum class AppLanguage(val tag: String) {
    BANGLA("bn"),
    ENGLISH("en"),
}

data class UserPreferenceState(
    val language: AppLanguage = AppLanguage.BANGLA,
    val lastPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val guestWarningDismissed: Boolean = false,
    val localBackupEnabled: Boolean = false,
    val localBackupUri: String? = null,
    val driveBackupEnabled: Boolean = false,
    val lastLocalBackupAtEpochMillis: Long? = null,
)

private val Context.hisabPreferencesDataStore by preferencesDataStore(name = UserPreferences.FILE_NAME)

class UserPreferences private constructor(
    private val dataStore: DataStore<Preferences>,
) : PaymentMethodPreference, LocalBackupPreferences {
    override val state: Flow<UserPreferenceState> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            UserPreferenceState(
                language = preferences[LANGUAGE]?.let { stored -> AppLanguage.entries.find { it.name == stored } }
                    ?: AppLanguage.BANGLA,
                lastPaymentMethod = preferences[LAST_PAYMENT_METHOD]
                    ?.let { stored -> PaymentMethod.entries.find { it.name == stored } }
                    ?: PaymentMethod.CASH,
                guestWarningDismissed = preferences[GUEST_WARNING_DISMISSED] ?: false,
                localBackupEnabled = preferences[LOCAL_BACKUP_ENABLED] ?: false,
                localBackupUri = preferences[LOCAL_BACKUP_URI],
                driveBackupEnabled = preferences[DRIVE_BACKUP_ENABLED] ?: false,
                lastLocalBackupAtEpochMillis = preferences[LAST_LOCAL_BACKUP_AT],
            )
        }

    suspend fun setLanguage(language: AppLanguage) = update(LANGUAGE, language.name)

    override suspend fun get(): PaymentMethod = state.first().lastPaymentMethod

    override suspend fun set(method: PaymentMethod) = setLastPaymentMethod(method)

    suspend fun setLastPaymentMethod(method: PaymentMethod) = update(LAST_PAYMENT_METHOD, method.name)

    suspend fun dismissGuestWarning() {
        dataStore.edit { it[GUEST_WARNING_DISMISSED] = true }
    }

    override suspend fun setLocalBackup(enabled: Boolean, uri: String?) {
        dataStore.edit { preferences ->
            preferences[LOCAL_BACKUP_ENABLED] = enabled
            if (uri == null) preferences.remove(LOCAL_BACKUP_URI) else preferences[LOCAL_BACKUP_URI] = uri
        }
    }

    override suspend fun setLastLocalBackupAt(epochMillis: Long?) {
        dataStore.edit { preferences ->
            if (epochMillis == null) preferences.remove(LAST_LOCAL_BACKUP_AT) else preferences[LAST_LOCAL_BACKUP_AT] = epochMillis
        }
    }

    suspend fun setDriveBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[DRIVE_BACKUP_ENABLED] = enabled }
    }

    suspend fun clearPrivateState() {
        dataStore.edit { preferences ->
            preferences.remove(LAST_PAYMENT_METHOD)
            preferences.remove(GUEST_WARNING_DISMISSED)
            preferences.remove(LOCAL_BACKUP_ENABLED)
            preferences.remove(LOCAL_BACKUP_URI)
            preferences.remove(DRIVE_BACKUP_ENABLED)
            preferences.remove(LAST_LOCAL_BACKUP_AT)
        }
    }

    private suspend fun update(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    companion object {
        const val FILE_NAME = "hisab_user_preferences"
        private val LANGUAGE = stringPreferencesKey("language")
        private val LAST_PAYMENT_METHOD = stringPreferencesKey("last_payment_method")
        private val GUEST_WARNING_DISMISSED = booleanPreferencesKey("guest_warning_dismissed")
        private val LOCAL_BACKUP_ENABLED = booleanPreferencesKey("local_backup_enabled")
        private val LOCAL_BACKUP_URI = stringPreferencesKey("local_backup_uri")
        private val DRIVE_BACKUP_ENABLED = booleanPreferencesKey("drive_backup_enabled")
        private val LAST_LOCAL_BACKUP_AT = androidx.datastore.preferences.core.longPreferencesKey("last_local_backup_at")

        fun create(context: Context): UserPreferences = UserPreferences(context.applicationContext.hisabPreferencesDataStore)
    }
}
