package com.mrdastbaaf.mdbapp.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.ConnectivityObserver
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.home.domain.model.ServerInfo
import com.mrdastbaaf.mdbapp.features.home.domain.repository.VersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the P0 scaffold screen.
 *
 * Its real job is to prove the whole stack end-to-end: Hilt injects the
 * repository, the repository goes through OkHttp and Retrofit to the live
 * WordPress bridge, and the result lands in a [UiState] that the screen renders.
 * If this screen shows the plugin version, then DNS, TLS, the WAF, LiteSpeed,
 * WordPress, the plugin's CORS filter and our interceptor chain are all working.
 *
 * The same shape — `StateFlow<UiState<T>>` + `refresh()` — is what every later
 * feature ViewModel will use.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val versionRepository: VersionRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ServerInfo>>(UiState.Loading)
    val state: StateFlow<UiState<ServerInfo>> = _state.asStateFlow()

    /**
     * Inverted for the UI: the banner shows when we are *not* online.
     *
     * `stateIn` rather than a manual collection loop — it gives a cold flow a
     * hot, lifecycle-independent value with a sensible initial state, which is
     * exactly what a banner needs.
     */
    val isOffline: StateFlow<Boolean> = connectivityObserver.isOnline
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = !connectivityObserver.currentlyOnline(),
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading

            versionRepository.fetchServerInfo()
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { throwable ->
                    _state.value = UiState.Error(throwable as? Failure ?: Failure.Unknown(throwable))
                }
        }
    }
}
