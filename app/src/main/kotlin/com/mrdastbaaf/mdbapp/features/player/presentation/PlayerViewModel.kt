package com.mrdastbaaf.mdbapp.features.player.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.features.player.domain.model.PlaybackSession
import com.mrdastbaaf.mdbapp.features.player.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlayerUiState {

    data object Loading : PlayerUiState

    data class Ready(
        val session: PlaybackSession,
        val startPositionMillis: Long,
    ) : PlayerUiState

    data class Error(val failure: Failure) : PlayerUiState
}

/**
 * Opens a lesson and keeps the server's idea of the playback position current.
 *
 * The access decision is not made here — [PlayerRepository.openLesson] asks the
 * server, and a refusal arrives as a 403 whose Persian message is displayed
 * verbatim. If the ticket is rejected the screen shows that text, which is more
 * accurate than anything the client could invent.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PlayerRepository,
) : ViewModel() {

    private val lessonId: String = savedStateHandle.get<String>(ARG_LESSON_ID).orEmpty()

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /** Throttle: the position is written at most once per [SAVE_INTERVAL_SECONDS]. */
    private var lastSavedSeconds = -1

    init {
        open()
    }

    fun open() {
        if (lessonId.isBlank()) {
            _state.value = PlayerUiState.Error(Failure.NotFound())
            return
        }

        viewModelScope.launch {
            _state.value = PlayerUiState.Loading

            val session = repository.openLesson(lessonId).getOrElse { throwable ->
                _state.value = PlayerUiState.Error(
                    throwable as? Failure ?: Failure.Unknown(cause = throwable),
                )
                return@launch
            }

            // A missing progress endpoint (404) is not an error: it just means
            // this server predates the patch, so playback starts at zero.
            val resumeSeconds = repository.progress(lessonId).getOrNull()?.resumePositionSeconds ?: 0

            _state.value = PlayerUiState.Ready(
                session = session,
                startPositionMillis = resumeSeconds * 1_000L,
            )
        }
    }

    /** Called by the player every few seconds while it is actually playing. */
    fun onPositionChanged(positionMillis: Long, durationMillis: Long) {
        if (durationMillis <= 0L) return

        val seconds = (positionMillis / 1_000L).toInt()
        if (seconds <= 0) return
        if (lastSavedSeconds >= 0 && seconds - lastSavedSeconds < SAVE_INTERVAL_SECONDS) return

        lastSavedSeconds = seconds
        persist(seconds, (durationMillis / 1_000L).toInt())
    }

    /** Called when the screen goes away, so the last few seconds are not lost. */
    fun onStopped(positionMillis: Long, durationMillis: Long) {
        if (durationMillis <= 0L) return

        val seconds = (positionMillis / 1_000L).toInt()
        if (seconds <= 0) return

        lastSavedSeconds = seconds
        persist(seconds, (durationMillis / 1_000L).toInt())
    }

    private fun persist(positionSeconds: Int, durationSeconds: Int) {
        viewModelScope.launch {
            // Fire and forget: losing a progress write must never surface to
            // the user or interrupt what they are watching.
            repository.saveProgress(lessonId, positionSeconds, durationSeconds)
        }
    }

    companion object {
        /** Must match the `{lessonId}` placeholder in `Routes.PLAYER`. */
        const val ARG_LESSON_ID = "lessonId"

        private const val SAVE_INTERVAL_SECONDS = 10
    }
}
