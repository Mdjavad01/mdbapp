package com.mrdastbaaf.mdbapp.core.ui.state

import com.mrdastbaaf.mdbapp.core.error.Failure

/**
 * The four states every screen in this app must handle.
 *
 * The brief requires loading / empty / error / offline on every screen, and the
 * way to make that structural rather than aspirational is to make it impossible
 * to render a screen without answering the question. Screens therefore expose a
 * `StateFlow<UiState<T>>` and render it through
 * [com.mrdastbaaf.mdbapp.core.ui.component.AsyncStateView], which has no branch
 * that can be silently omitted.
 *
 * `Offline` is deliberately *not* a member: connectivity is orthogonal to
 * whether the last load succeeded, and a cached list should stay readable while
 * the device is offline. The offline banner is driven separately by
 * [com.mrdastbaaf.mdbapp.core.network.ConnectivityObserver], so a screen can be
 * `Success` and offline at the same time — which is the correct behaviour.
 */
sealed interface UiState<out T> {

    /** First load in flight. Only used when there is nothing to show yet. */
    data object Loading : UiState<Nothing>

    /** The request succeeded but returned nothing meaningful. */
    data class Empty(val message: String? = null) : UiState<Nothing>

    /** The request failed. [Failure.userMessage] is safe to display. */
    data class Error(val failure: Failure) : UiState<Nothing>

    /** Data to render. */
    data class Success<T>(val data: T) : UiState<T>

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
}

/** Transforms the payload while preserving every non-success state. */
inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Success -> UiState.Success(transform(data))
    is UiState.Empty -> this
    is UiState.Error -> this
    UiState.Loading -> UiState.Loading
}

/** Runs [transform] and turns an empty result into [UiState.Empty]. */
inline fun <T, R : Collection<*>> UiState<T>.mapToCollection(
    emptyMessage: String? = null,
    transform: (T) -> R,
): UiState<R> = when (this) {
    is UiState.Success -> transform(data)
        .takeIf { it.isNotEmpty() }
        ?.let { UiState.Success(it) }
        ?: UiState.Empty(emptyMessage)

    is UiState.Empty -> this
    is UiState.Error -> this
    UiState.Loading -> UiState.Loading
}

/** The current data, or `null` when there is none yet. */
val <T> UiState<T>.dataOrNull: T?
    get() = (this as? UiState.Success)?.data
