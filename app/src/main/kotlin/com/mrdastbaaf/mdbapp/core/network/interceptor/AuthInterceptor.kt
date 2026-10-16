package com.mrdastbaaf.mdbapp.core.network.interceptor

import com.mrdastbaaf.mdbapp.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the Bearer token and reacts to its rejection.
 *
 * Two responsibilities that belong together, because both are "the wire-level
 * half of the session":
 *
 *  1. **Request** — adds `Authorization: Bearer {user_id}.{secret}` when a token
 *     exists. Public endpoints (`/courses`, `/shop/products`, `/app-ui`, …) work
 *     without it, but several of them change their answer when it is present:
 *     `GET /courses` only returns `owned: true` if the token is attached. So the
 *     header is added whenever we have one, never conditionally per-endpoint.
 *
 *  2. **Response** — on a 401 it invalidates the session. There is no refresh
 *     flow in this backend, so a 401 is terminal: the user re-authenticates with
 *     a fresh OTP.
 *
 * The login endpoints are excluded from (2). A 401 from `verify-otp` means the
 * credentials were wrong, not that an existing session died — wiping storage
 * there would be harmless today but would silently destroy a valid session if
 * the login flow were ever invoked while already signed in.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        val token = sessionManager.token()
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
        }

        val response = chain.proceed(request)

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED && !isAuthEndpoint(path)) {
            response.close()
            sessionManager.onUnauthorized()
        }

        return response
    }

    private fun isAuthEndpoint(path: String): Boolean =
        path.contains("/auth/request-otp") ||
            path.contains("/auth/verify-otp") ||
            path.contains("/auth/logout")

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
