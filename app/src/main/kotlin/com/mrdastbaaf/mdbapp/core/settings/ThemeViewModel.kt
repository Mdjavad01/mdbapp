package com.mrdastbaaf.mdbapp.core.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Exposes the theme choice to both the root (which applies it) and the settings
 * row (which changes it).
 *
 * Two instances of this ViewModel exist — one scoped to the Activity for the
 * root, one to the navigation entry for the settings row — and that is fine:
 * both read the same `DataStore`, so a change made in one is observed by the
 * other without any shared state.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val preference: StateFlow<ThemePreference> = themePreferences.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemePreference.SYSTEM,
    )

    fun select(preference: ThemePreference) {
        viewModelScope.launch { themePreferences.setTheme(preference) }
    }
}
