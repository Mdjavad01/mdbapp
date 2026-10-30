package com.mrdastbaaf.mdbapp.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Makes the anonymous catalogue endpoints cacheable on the device.
 *
 * The bridge sends `Cache-Control: no-store` on every response in its namespace.
 * For authenticated data that is correct — nothing should sit in a shared cache.
 * But `/app-ui`, `/shop/products` and `/memberships/plans` are public and change
 * when the owner edits the panel, not per second, so `no-store` costs a full PHP
 * bootstrap and a handful of MySQL queries on **every launch, for every user**.
 *
 * This rewrites the headers of those three responses to a 60-second public
 * cache, which OkHttp then serves from disk. Two consequences worth stating:
 *
 *  - A cold launch still costs one request; a warm one within the minute costs
 *    none, and works with no signal.
 *  - The owner's edit can take up to a minute to appear. That is the trade, and
 *    it is why the window is a minute and not an hour.
 *
 * Deliberately a *network* interceptor: the cache sits below it, so the rewrite
 * is what gets stored. An application interceptor would rewrite the response
 * after the cache had already decided not to keep it.
 */
@Singleton
class PublicCacheInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (request.method != "GET" || !response.isSuccessful) return response
        val path = request.url.encodedPath
        if (PUBLIC_PATHS.none { path.endsWith(it) }) return response

        return response.newBuilder()
            .removeHeader("Cache-Control")
            .removeHeader("Pragma")
            .removeHeader("Expires")
            // LiteSpeed's own header would otherwise keep its edge from caching
            // a response the client is happy to keep.
            .removeHeader("X-LiteSpeed-Cache-Control")
            .header("Cache-Control", "public, max-age=$TTL_SECONDS")
            .build()
    }

    private companion object {
        const val TTL_SECONDS = 60

        /** Anonymous, owner-edited, and identical for every user. */
        val PUBLIC_PATHS = listOf(
            "/app-ui",
            "/shop/products",
            "/memberships/plans",
        )
    }
}
