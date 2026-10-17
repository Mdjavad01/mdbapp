package com.mrdastbaaf.mdbapp.features.catalogue.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.ConnectivityObserver
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Course
import com.mrdastbaaf.mdbapp.features.catalogue.domain.repository.CatalogueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which list the catalogue tab is showing. */
enum class CatalogueMode { ALL, MINE }

data class CatalogueUiState(
    val mode: CatalogueMode = CatalogueMode.ALL,
    val courses: UiState<List<Course>> = UiState.Loading,
)

/**
 * Backs the catalogue tab.
 *
 * The two lists come from different endpoints with different semantics:
 * `/courses` is public and reports `owned` per item, `/my/courses` is
 * authenticated and returns only what the user paid for. They are kept as one
 * screen with a toggle because that is how the question is actually asked —
 * "show me everything" or "show me what I have" — and switching re-loads rather
 * than filtering client-side, so `owned` can never drift from the server's view.
 */
@HiltViewModel
class CatalogueViewModel @Inject constructor(
    private val repository: CatalogueRepository,
    private val sessionManager: SessionManager,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogueUiState())
    val state: StateFlow<CatalogueUiState> = _state.asStateFlow()

    val isOffline: StateFlow<Boolean> = connectivityObserver.isOnline
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = !connectivityObserver.currentlyOnline(),
        )

    init {
        load(CatalogueMode.ALL)
    }

    fun selectMode(mode: CatalogueMode) {
        if (mode == _state.value.mode) return
        _state.update { it.copy(mode = mode) }
        load(mode)
    }

    fun refresh() = load(_state.value.mode)

    private fun load(mode: CatalogueMode) {
        viewModelScope.launch {
            // "My courses" needs a session. Asking the server without one would
            // earn a 401, which the auth interceptor turns into a forced logout —
            // a hostile way to answer "you have no courses".
            if (mode == CatalogueMode.MINE && !sessionManager.isSignedIn()) {
                _state.update { it.copy(courses = UiState.Empty()) }
                return@launch
            }

            _state.update { it.copy(courses = UiState.Loading) }

            val result = when (mode) {
                CatalogueMode.ALL -> repository.getCourses()
                CatalogueMode.MINE -> repository.getMyCourses()
            }

            _state.update {
                it.copy(
                    courses = result.fold(
                        onSuccess = { courses ->
                            if (courses.isEmpty()) {
                                UiState.Empty()
                            } else {
                                UiState.Success(courses)
                            }
                        },
                        onFailure = { throwable ->
                            UiState.Error(
                                throwable as? Failure ?: Failure.Unknown(cause = throwable),
                            )
                        },
                    ),
                )
            }
        }
    }
}
