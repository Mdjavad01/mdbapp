package com.mrdastbaaf.mdbapp.core.network

import com.mrdastbaaf.mdbapp.core.constants.ApiPaths
import com.mrdastbaaf.mdbapp.core.network.dto.VersionDto
import retrofit2.http.GET

/**
 * Retrofit description of the WordPress bridge.
 *
 * Only endpoints implemented so far are declared here; the rest of the contract
 * (auth, catalogue, membership, playback) is added phase by phase. Every path
 * comes from [ApiPaths] so there is exactly one place to change when the server
 * adds a route.
 *
 * Endpoints returning a *bare array* (`/courses`, `/memberships/plans`,
 * `/app-pages`) or a *bare object* (`/courses/{slug}`) are declared with that
 * exact return type — the bridge is inconsistent about envelopes and pretending
 * otherwise would only move the problem into a wrapper that sometimes lies.
 */
interface MdbApi {

    /**
     * Health/version probe. Public, cheap, and the only call the app makes
     * before a user signs in.
     */
    @GET(ApiPaths.VERSION)
    suspend fun getVersion(): VersionDto
}
