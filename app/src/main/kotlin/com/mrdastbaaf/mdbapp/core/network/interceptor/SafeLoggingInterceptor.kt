package com.mrdastbaaf.mdbapp.core.network.interceptor

import android.util.Log
import com.mrdastbaaf.mdbapp.core.config.AppConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A deliberately impoverished logger.
 *
 * The security brief forbids logging tokens, tickets and video URLs. Rather than
 * configuring a general-purpose logger to redact those, this logs **only** the
 * method, the sanitised path, the status code and the duration. No headers, no
 * request body, no response body — which makes it impossible to leak a
 * credential through this channel by accident, now or after a future edit.
 *
 * It is installed only when [AppConfig.httpLoggingEnabled] is true, i.e. in dev
 * and staging builds. The class itself is also stripped from release by R8's
 * `-assumenosideeffects` rule on `android.util.Log`.
 */
@Singleton
class SafeLoggingInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.nanoTime()

        return try {
            val response = chain.proceed(request)
            val millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            Log.i(
                TAG,
                "${request.method} ${sanitise(request.url.toString())} -> ${response.code} (${millis}ms)",
            )
            response
        } catch (throwable: Throwable) {
            val millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            Log.w(
                TAG,
                "${request.method} ${sanitise(request.url.toString())} failed after ${millis}ms: " +
                    throwable.javaClass.simpleName,
            )
            throw throwable
        }
    }

    /**
     * Removes anything that could carry a credential.
     *
     * Two vectors: the `token` query parameter (used by the PHP stream proxy) and
     * any path segment that looks like a signed URL. The origin and the endpoint
     * path are kept because that is the part that is actually useful for
     * debugging.
     */
    private fun sanitise(url: String): String {
        val withoutQuery = url.substringBefore('?')
        val hadQuery = url.contains('?')
        val redacted = if (hadQuery) "$withoutQuery?<redacted>" else withoutQuery
        return redacted.replace(Regex("/(tkt_|sess_)[A-Za-z0-9_-]+"), "/\$1<redacted>")
    }

    private companion object {
        const val TAG = "MdbNet"
    }
}
