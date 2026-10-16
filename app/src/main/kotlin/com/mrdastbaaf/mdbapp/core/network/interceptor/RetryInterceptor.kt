package com.mrdastbaaf.mdbapp.core.network.interceptor

import com.mrdastbaaf.mdbapp.core.config.AppConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retries **idempotent** requests only.
 *
 * GET is retried; POST/PUT/DELETE never are. That is not a stylistic choice —
 * `POST /auth/verify-otp` consumes the OTP and `POST /lessons/{id}/playback`
 * consumes the ticket, so a blind retry would burn a one-time credential and
 * surface a confusing failure to the user.
 *
 * Retried conditions, matching what the bridge and its host actually produce:
 *  - transport failures (no connectivity, TLS reset, read timeout)
 *  - 502 / 503 / 504 — the WordPress host is behind a WAF and a shared PHP
 *    worker pool, so transient 5xx responses are routine
 *
 * 4xx is never retried: it is a contract answer, not a hiccup.
 */
@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isIdempotent = request.method.equals("GET", ignoreCase = true)

        if (!isIdempotent) return chain.proceed(request)

        var attempt = 0
        var lastResponse: Response? = null
        var lastError: IOException? = null

        while (attempt <= AppConfig.MAX_GET_RETRIES) {
            // Close the previous attempt's body before retrying, or the connection
            // pool leaks and OkHttp eventually refuses new connections.
            lastResponse?.close()

            try {
                val response = chain.proceed(request)
                if (!response.isTransientFailure()) return response

                lastResponse = response
            } catch (io: IOException) {
                lastError = io
            }

            if (attempt == AppConfig.MAX_GET_RETRIES) break
            attempt++
            sleepBackoff(attempt)
        }

        // Out of retries: return the last 5xx if we had one, otherwise rethrow.
        return lastResponse ?: throw (lastError ?: IOException("Request failed after retries"))
    }

    private fun Response.isTransientFailure(): Boolean = when (code) {
        HttpURLConnection.HTTP_BAD_GATEWAY,
        HttpURLConnection.HTTP_UNAVAILABLE,
        HttpURLConnection.HTTP_GATEWAY_TIMEOUT,
        -> true

        else -> false
    }

    /** Linear backoff: 600 ms, then 1200 ms. */
    private fun sleepBackoff(attempt: Int) {
        runCatching { Thread.sleep(AppConfig.RETRY_BASE_DELAY_MS * attempt) }
    }
}
