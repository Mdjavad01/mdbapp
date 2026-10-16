package com.mrdastbaaf.mdbapp.core.ui.state

import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.error.Failure

/**
 * Turns a [Failure] into something displayable.
 *
 * A server-supplied message always wins. The bridge returns human-quality Persian
 * for every error code it documents — `invalid_otp` even appends the remaining
 * attempt count, and `rate_limited` names which quota was hit — so replacing that
 * text with a generic client string would throw away the most useful part of the
 * response.
 *
 * Only when the server said nothing (a transport failure, a gateway HTML page)
 * does a localised default get chosen from the failure type. Either way the user
 * never sees a blank error or an English string.
 */
fun Failure.toUiMessage(): UiMessage {
    userMessage?.takeIf { it.isNotBlank() }?.let { return UiMessage.Text(it) }

    return when (this) {
        is Failure.NoConnection -> UiMessage.Res(R.string.error_no_connection)
        is Failure.Timeout -> UiMessage.Res(R.string.error_timeout)
        is Failure.RateLimited -> UiMessage.Res(R.string.error_rate_limited)
        is Failure.InvalidResponse -> UiMessage.Res(R.string.error_invalid_response)
        is Failure.Server -> UiMessage.Res(R.string.error_server)
        else -> UiMessage.Res(R.string.error_unknown)
    }
}

/** Convenience for the `onFailure { }` branch of a `Result`. */
fun Throwable.toUiMessage(): UiMessage =
    (this as? Failure)?.toUiMessage() ?: UiMessage.Res(R.string.error_unknown)
