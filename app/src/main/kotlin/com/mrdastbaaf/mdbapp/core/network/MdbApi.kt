package com.mrdastbaaf.mdbapp.core.network

import com.mrdastbaaf.mdbapp.core.constants.ApiPaths
import com.mrdastbaaf.mdbapp.core.network.dto.VersionDto
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.LogoutResponse
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.RequestOtpRequest
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.RequestOtpResponse
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.UserProfileDto
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.VerifyOtpRequest
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.VerifyOtpResponse
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.CourseDetailDto
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.CourseDto
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.MyCoursesDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit description of the WordPress bridge.
 *
 * Endpoints are declared phase by phase as they are implemented. Every path
 * comes from [ApiPaths], so there is exactly one place to change when the server
 * adds or moves a route.
 *
 * Endpoints returning a *bare array* (`/courses`, `/memberships/plans`,
 * `/app-pages`) or a *bare object* (`/courses/{slug}`) are declared with that
 * exact return type — the bridge is inconsistent about envelopes and pretending
 * otherwise would only move the problem into a wrapper that sometimes lies.
 */
interface MdbApi {

    // ── Diagnostics ─────────────────────────────────────────────────────────

    /**
     * Health/version probe. Public, cheap, and the only call the app makes
     * before a user signs in.
     */
    @GET(ApiPaths.VERSION)
    suspend fun getVersion(): VersionDto

    // ── Authentication ──────────────────────────────────────────────────────

    /**
     * Sends an OTP by SMS. Public.
     *
     * The SMS is dispatched **asynchronously** in a separate PHP process, so
     * this response returns before the message leaves the server. "No SMS yet"
     * is not an error.
     *
     * Rate limits: 2 minutes between codes per phone, 5/hour/phone,
     * 15/day/phone, 10/hour/IP — all surfaced as `429 rate_limited` with a
     * Persian explanation.
     */
    @POST(ApiPaths.AUTH_REQUEST_OTP)
    suspend fun requestOtp(@Body body: RequestOtpRequest): RequestOtpResponse

    /**
     * Verifies the OTP and issues the Bearer token. Public.
     *
     * An unknown phone number is **auto-registered** as a subscriber, which is
     * why the request optionally carries a name.
     *
     * A wrong code returns **400 `invalid_otp`**, deliberately not 401 — the
     * client would otherwise treat it as an expired session. Five wrong attempts
     * burn the code and return `429 otp_locked`.
     */
    @POST(ApiPaths.AUTH_VERIFY_OTP)
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): VerifyOtpResponse

    /** Current profile. Requires the Bearer token. */
    @GET(ApiPaths.AUTH_ME)
    suspend fun getMe(): UserProfileDto

    /**
     * Revokes the Bearer token server-side. Requires the token.
     *
     * Always clear local storage afterwards regardless of the outcome — a
     * failure here means the token was already invalid, which is the state we
     * wanted anyway.
     */
    @POST(ApiPaths.AUTH_LOGOUT)
    suspend fun logout(): LogoutResponse

    // ── Catalogue ───────────────────────────────────────────────────────────

    /**
     * The public catalogue. Returns a **bare array** — no envelope.
     *
     * The route never rejects an anonymous caller; it simply reports
     * `owned: false` for everything. The auth interceptor attaches the token
     * when there is one, which is what makes `owned` meaningful.
     *
     * Capped at 50 items, newest first, with no pagination parameter.
     */
    @GET(ApiPaths.COURSES)
    suspend fun getCourses(): List<CourseDto>

    /**
     * One course with its lesson list. Returns a **bare object**.
     *
     * `slug` is the product `post_name`. Retrofit encodes it, and the bridge
     * tolerates raw, single- and double-encoded forms — every filename in this
     * project is Persian, so this matters.
     */
    @GET(ApiPaths.COURSE_DETAIL_TEMPLATE)
    suspend fun getCourse(@Path("slug") slug: String): CourseDetailDto

    /** The signed-in user's courses. Enveloped: `{ success, courses, count }`. */
    @GET(ApiPaths.MY_COURSES)
    suspend fun getMyCourses(): MyCoursesDto
}
