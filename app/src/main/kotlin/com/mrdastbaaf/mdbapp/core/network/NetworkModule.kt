package com.mrdastbaaf.mdbapp.core.network

import com.mrdastbaaf.mdbapp.core.config.AppConfig
import com.mrdastbaaf.mdbapp.core.network.interceptor.AuthInterceptor
import com.mrdastbaaf.mdbapp.core.network.interceptor.RetryInterceptor
import com.mrdastbaaf.mdbapp.core.network.interceptor.SafeLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * JSON policy.
     *
     *  - `ignoreUnknownKeys` — the bridge adds response fields without a version
     *    bump, and the app must not break when it does.
     *  - `explicitNulls = false` — keeps request bodies free of `"field": null`
     *    entries, which the plugin's `get_param()` treats as an absent parameter.
     *    This matters: `PUT /me/progress` uses presence to decide what to
     *    replace, so sending an explicit null would be a different request.
     *  - `coerceInputValues` — a `null` where a non-null default exists falls
     *    back to the default instead of throwing.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = false
    }

    /**
     * Interceptor order is deliberate:
     *
     *   1. [SafeLoggingInterceptor] — outermost, so the logged duration includes
     *      every retry and reflects what the user actually experienced.
     *   2. [RetryInterceptor]      — retries GETs on transport errors and 5xx.
     *   3. [AuthInterceptor]       — innermost, closest to the wire: it decorates
     *      the request with the token and, on 401, invalidates the session.
     *
     * There is intentionally no cache-busting interceptor. The web client appends
     * `?_=timestamp` to defeat LiteSpeed, but that only matters for a browser's
     * shared cache. The plugin already sends `Cache-Control: no-store` and
     * `X-LiteSpeed-Cache-Control: no-cache` on every response in the namespace,
     * so a native client has nothing to defeat.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        retryInterceptor: RetryInterceptor,
        safeLoggingInterceptor: SafeLoggingInterceptor,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(safeLoggingInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(authInterceptor)

        // Certificate pinning is opt-in at build time; see res/xml/network_security_config.xml
        // for the pin values and the rotation warning.
        if (AppConfig.certPinningEnabled) {
            PinningConfig.apply(builder)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(AppConfig.baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideMdbApi(retrofit: Retrofit): MdbApi = retrofit.create(MdbApi::class.java)
}
