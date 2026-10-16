package com.mrdastbaaf.mdbapp.core.error

/**
 * Every failure the app can surface, as a closed set.
 *
 * Modelled as a [Throwable] hierarchy on purpose: it lets a `Result<T>` carry a
 * failure through repository boundaries without a bespoke `Either` type, while
 * `exceptionOrNull() as? Failure` keeps the exhaustive `when` in the UI layer
 * honest. A separate `Outcome` type would work too, but it would mean either a
 * second result type alongside `Result`, or `Result<Result<T>>` — both worse.
 *
 * Repositories return `Result<T>`; ViewModels translate the [Failure] into a
 * `UiState`. Nothing below the presentation layer ever touches an HTTP status
 * code or a raw exception, which is what keeps the layers testable without a
 * network.
 */
sealed class Failure(
    /**
     * A message that is safe and useful to show the user, already in Persian.
     *
     * Where the server supplied a message it is passed through verbatim: the
     * WordPress bridge returns human-quality Persian for every error code it
     * documents, so rewriting it client-side would only lose information.
     */
    val userMessage: String?,
    cause: Throwable? = null,
) : Exception(userMessage, cause) {

    /** No connectivity, DNS failure, connection refused, TLS failure. */
    class NoConnection(userMessage: String? = null) : Failure(userMessage)

    /** The request exceeded the configured read timeout. */
    class Timeout(userMessage: String? = null) : Failure(userMessage)

    /**
     * 401. The token is missing, revoked, or past its 30-day lifetime.
     * The auth interceptor reacts by wiping the session; the UI routes to login.
     */
    class Unauthorized(userMessage: String? = null) : Failure(userMessage)

    /**
     * 403. Authenticated but not entitled — the lesson requires a higher tier,
     * or a non-student role tried to sign in.
     */
    class Forbidden(userMessage: String? = null, val code: String? = null) : Failure(userMessage)

    /** 404. */
    class NotFound(userMessage: String? = null) : Failure(userMessage)

    /** 409, e.g. `registration_conflict`. */
    class Conflict(userMessage: String? = null, val code: String? = null) : Failure(userMessage)

    /**
     * 429. The bridge rate-limits OTP requests (2 min / phone, 5 per hour,
     * 15 per day, 10 per hour per IP) and playback tickets (30 per hour). Its
     * message names the quota that was hit, so it is always shown as-is.
     */
    class RateLimited(userMessage: String? = null) : Failure(userMessage)

    /** 400/422 with a validation code, e.g. `invalid_phone` or `invalid_otp`. */
    class Validation(userMessage: String? = null, val code: String? = null) : Failure(userMessage)

    /** 5xx, or a documented error code we did not anticipate. */
    class Server(userMessage: String? = null, val httpStatus: Int? = null) : Failure(userMessage)

    /** The body did not parse, or was missing fields the contract requires. */
    class InvalidResponse(userMessage: String? = null) : Failure(userMessage)

    /** Anything genuinely unexpected. The cause is for logs, never for the UI. */
    class Unknown(userMessage: String? = null, cause: Throwable? = null) : Failure(userMessage, cause)

    /** True when retrying the same action could plausibly succeed. */
    val isRetryable: Boolean
        get() = this is NoConnection || this is Timeout || this is Server
}
