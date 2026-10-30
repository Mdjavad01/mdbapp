package com.mrdastbaaf.mdbapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.HomeConfig
import com.mrdastbaaf.mdbapp.features.appcontent.domain.repository.AppContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The one place the app asks for `/app-ui` on its own behalf.
 *
 * The shell needs it before any tab is drawn — the bottom bar is built from it —
 * so it cannot wait for the home screen. The repository deduplicates the request
 * and caches the result for a minute, so the home tab and the account card that
 * ask a moment later cost nothing.
 *
 * Failure is silent on purpose: a shell with the default four tabs is a working
 * app, and a navigation bar is not worth an error screen.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    content: AppContentRepository,
) : ViewModel() {

    private val _config = MutableStateFlow<HomeConfig?>(null)
    val config: StateFlow<HomeConfig?> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            content.home().onSuccess { _config.value = it }
        }
    }
}
