package com.mrdastbaaf.mdbapp.core.config

import com.mrdastbaaf.mdbapp.BuildConfig

/**
 * Build-time environment configuration.
 *
 * The base URL comes from the product flavour (`dev` / `staging` / `prod`) via
 * `buildConfigField`, so a release APK can never accidentally point at staging
 * or at a debug host.
 */
object AppConfig {

    /** Retrofit requires a trailing slash on the base URL. */
    val baseUrl: String = BuildConfig.API_BASE_URL

    val environment: Environment = when {
        BuildConfig.FLAVOR == "prod" -> Environment.PRODUCTION
        BuildConfig.FLAVOR == "staging" -> Environment.STAGING
        else -> Environment.DEVELOPMENT
    }

    val isDebugBuild: Boolean = BuildConfig.DEBUG

    val versionName: String = BuildConfig.VERSION_NAME
    val versionCode: Int = BuildConfig.VERSION_CODE

    /**
     * OkHttp logging is compiled into debug and dev builds only. Even then it is
     * routed through [com.mrdastbaaf.mdbapp.core.network.interceptor.SafeLoggingInterceptor],
     * which redacts the Authorization header and every media URL.
     */
    val httpLoggingEnabled: Boolean = BuildConfig.ENABLE_HTTP_LOGGING

    /**
     * When true, root/emulator detection blocks playback and paid content.
     * Relaxed on the dev flavour so the team can work on emulators.
     */
    val strictSecurityEnabled: Boolean = BuildConfig.ENABLE_STRICT_SECURITY

    val certPinningEnabled: Boolean = BuildConfig.ENABLE_CERT_PINNING

    /** Connect / read / write timeouts. `request-otp` can take a few seconds. */
    const val CONNECT_TIMEOUT_SECONDS: Long = 20
    const val READ_TIMEOUT_SECONDS: Long = 30
    const val WRITE_TIMEOUT_SECONDS: Long = 30

    /** GET requests are retried on transient failures; POST/PUT never are. */
    const val MAX_GET_RETRIES: Int = 2
    const val RETRY_BASE_DELAY_MS: Long = 600L

    /**
     * The website's purchase/renewal page. The app never sells subscriptions —
     * it hands off to the browser, as the brief requires.
     */
    const val MEMBERSHIP_URL: String = "https://app.mrdastbaaf.com/#/membership"
}

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
}
