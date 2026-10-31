package com.mrdastbaaf.mdbapp.core.network.interceptor

import com.mrdastbaaf.mdbapp.core.config.AppConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells the bridge that this request came from the app.
 *
 * Until now a purchase made in the app and one made on the website were
 * indistinguishable: the bridge hardcoded `created_via` to its web-app value and
 * wrote «از وب‌اپ» on every order note, so the owner could not tell which channel
 * a sale came from.
 *
 * The header is the honest answer to that question — the app states what it is
 * rather than being guessed from a user agent. The bridge still falls back to
 * the OkHttp user agent for builds that predate this header.
 *
 * Version is included because it is the first thing worth knowing when a client
 * misbehaves: `android/1.7.0`.
 */
@Singleton
class ClientInfoInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header(HEADER_CLIENT, "$CLIENT_ANDROID/${AppConfig.versionName}")
            .build()

        return chain.proceed(request)
    }

    private companion object {
        const val HEADER_CLIENT = "X-MDB-Client"
        const val CLIENT_ANDROID = "android"
    }
}
