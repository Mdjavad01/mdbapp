package com.mrdastbaaf.mdbapp.features.auth.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.core.session.SessionState
import com.mrdastbaaf.mdbapp.core.ui.state.UiMessage
import com.mrdastbaaf.mdbapp.core.ui.state.toUiMessage
import com.mrdastbaaf.mdbapp.features.auth.data.CurrentUserStore
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserProfile
import com.mrdastbaaf.mdbapp.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The signed-in identity, for any screen that shows it.
 *
 * On creation, if a token exists it re-fetches `GET /auth/me`. That is not just
 * a refresh — it is the session re-validation. The token is opaque and the client
 * cannot read its expiry, so the only way to find out whether a 30-day token is
 * still good is to use it. If it has expired, the call returns 401, the auth
 * interceptor wipes the session, and the UI falls back to signed-out with no
 * special-case code here.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<UiMessage?>(null)

    val state: StateFlow<SessionUiState> = combine(
        sessionManager.state,
        currentUserStore.profile,
        _isRefreshing,
        _errorMessage,
    ) { sessionState, profile, refreshing, error ->
        SessionUiState(
            isSignedIn = sessionState == SessionState.SignedIn,
            profile = profile,
            isRefreshing = refreshing,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SessionUiState(isSignedIn = sessionManager.isSignedIn()),
    )

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        if (!sessionManager.isSignedIn()) return

        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            authRepository.fetchProfile()
                .onFailure { throwable ->
                    _errorMessage.value = throwable.toUiMessage()
                }

            _isRefreshing.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            // The repository clears the session even if the network call fails:
            // a user who asked to sign out must end up signed out.
            authRepository.logout()
        }
    }

    fun onErrorShown() {
        _errorMessage.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

data class SessionUiState(
    val isSignedIn: Boolean = false,
    val profile: UserProfile? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: UiMessage? = null,
)
