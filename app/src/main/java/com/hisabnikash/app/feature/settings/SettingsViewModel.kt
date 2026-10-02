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

class SettingsViewModel(
    private val preferences: UserPreferences,
    private val dataDeletion: DataDeletionUseCase,
) : ViewModel() {
    val state: StateFlow<UserPreferenceState> = preferences.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferenceState(),
    )

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
}
