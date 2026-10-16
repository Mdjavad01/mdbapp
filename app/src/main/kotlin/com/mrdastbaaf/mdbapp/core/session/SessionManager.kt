package com.mrdastbaaf.mdbapp.core.session

import com.mrdastbaaf.mdbapp.core.storage.SecureTokenStore
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for "is anyone signed in".
 *
 * There is no refresh token in this backend: the opaque Bearer token is valid
 * for 30 days and, when it stops working, the only recovery is a fresh OTP
 * login. So this class has exactly two transitions — signed in, and signed out —
 * and every 401 funnels through [onUnauthorized] no matter which request caused
 * it.
 */
@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: SecureTokenStore,
) {

    private val _state = MutableStateFlow(
        if (tokenStore.hasToken()) SessionState.SignedIn else SessionState.SignedOut,
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /**
     * Emitted when the server rejects a token. The navigation layer collects
     * this once and routes to login; it is a broadcast, not a queue, because a
     * second concurrent 401 must not stack up two redirects.
     */
    private val _unauthorized = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val unauthorized: SharedFlow<Unit> = _unauthorized.asSharedFlow()

    /**
     * The current token, read synchronously.
     *
     * Called from the OkHttp interceptor thread, which is why this is a blocking
     * read rather than a `suspend` function. It is never exposed to the UI layer.
     */
    fun token(): String? = tokenStore.readToken()

    fun isSignedIn(): Boolean = _state.value == SessionState.SignedIn

    /** Called after a successful `verify-otp`. */
    fun onSignedIn(token: String) {
        tokenStore.writeToken(token)
        _state.value = SessionState.SignedIn
    }

    /** Called by the user-initiated logout, after `POST /auth/logout` returns. */
    fun onSignedOut() {
        tokenStore.clear()
        _state.value = SessionState.SignedOut
    }

    /**
     * Called by the network layer on a 401.
     *
     * Wipes the credential and notifies observers. Idempotent: repeated 401s
     * from parallel requests collapse into a single state change.
     */
    fun onUnauthorized() {
        if (_state.value == SessionState.SignedOut) return
        tokenStore.clear()
        _state.value = SessionState.SignedOut
        _unauthorized.tryEmit(Unit)
    }
}

enum class SessionState {
    /** A token exists in secure storage. It may still be rejected on the next call. */
    SignedIn,

    /** No token, or it was rejected. */
    SignedOut,
}
