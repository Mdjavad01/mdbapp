package com.mrdastbaaf.mdbapp.core.network

import com.mrdastbaaf.mdbapp.core.constants.ApiPaths
import com.mrdastbaaf.mdbapp.core.network.dto.VersionDto
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.LogoutResponse
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.RequestOtpRequest
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.RequestOtpResponse
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.UserProfileDto
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.VerifyOtpRequest
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.VerifyOtpResponse
import com.mrdastbaaf.mdbapp.features.account.data.remote.dto.MembershipPlanDto
import com.mrdastbaaf.mdbapp.features.appcontent.data.remote.dto.AppPageContentDto
import com.mrdastbaaf.mdbapp.features.appcontent.data.remote.dto.AppPageDto
import com.mrdastbaaf.mdbapp.features.appcontent.data.remote.dto.AppUiDto
import com.mrdastbaaf.mdbapp.features.account.data.remote.dto.OrdersDto
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.CourseDetailDto
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.CourseDto
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.MyCoursesDto
import com.mrdastbaaf.mdbapp.features.player.data.remote.dto.LessonProgressDto
import com.mrdastbaaf.mdbapp.features.player.data.remote.dto.LessonProgressRequest
import com.mrdastbaaf.mdbapp.features.player.data.remote.dto.PlaybackDto
import com.mrdastbaaf.mdbapp.features.player.data.remote.dto.PlaybackRequestDto
import com.mrdastbaaf.mdbapp.features.player.data.remote.dto.PlaybackTicketDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.PlanOrderRequestDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.ShopOrderRequestDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.ShopOrderResponseDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.ShopProductsDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.VerifyPaymentRequestDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.VerifyPaymentResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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
     * By numeric product id, not slug — see [ApiPaths.COURSE_BY_ID_TEMPLATE] for
     * why the slug route is unusable for Persian `post_name`s.
     */
    @GET(ApiPaths.COURSE_BY_ID_TEMPLATE)
    suspend fun getCourseById(@Path("productId") productId: Int): CourseDetailDto

    /** The signed-in user's courses. Enveloped: `{ success, courses, count }`. */
    @GET(ApiPaths.MY_COURSES)
    suspend fun getMyCourses(): MyCoursesDto

    // ── Account ─────────────────────────────────────────────────────────────

    /**
     * Purchasable plans. **Bare array**, cached 5 minutes server-side.
     *
     * Subscriptions are sold on the website; this call only renders them, and
     * the CTA opens the browser.
     */
    @GET(ApiPaths.MEMBERSHIP_PLANS)
    suspend fun getMembershipPlans(): List<MembershipPlanDto>

    /**
     * The user's purchases. Enveloped, and unusually **without** a `success`
     * key — the array is the whole contract.
     */
    @GET(ApiPaths.MY_ORDERS)
    suspend fun getMyOrders(): OrdersDto

    // ── App content ─────────────────────────────────────────────────────────

    /** Server-driven home: hero copy, section order, featured course ids. */
    @GET(ApiPaths.APP_UI)
    suspend fun getAppUi(): AppUiDto

    /** Guide pages the site publishes. **Bare array**, already access-filtered. */
    @GET(ApiPaths.APP_PAGES)
    suspend fun getAppPages(): List<AppPageDto>

    /** Rendered content for one page. 403 when the page is members-only. */
    @GET(ApiPaths.APP_PAGE_CONTENT_TEMPLATE)
    suspend fun getAppPageContent(@Path("pageId") pageId: String): AppPageContentDto

    // ── Shop & payment ──────────────────────────────────────────────────────

    /** The physical catalogue: a featured row plus products grouped by category. */
    @GET(ApiPaths.SHOP_PRODUCTS)
    suspend fun getShopProducts(): ShopProductsDto

    /**
     * Creates the order and returns the gateway URL.
     *
     * The app never sees card data: it opens [ShopOrderResponseDto.paymentUrl]
     * in a WebView, so the card form is the gateway's own page.
     */
    @POST(ApiPaths.PAYMENT_INITIATE)
    suspend fun initiateShopOrder(@Body body: ShopOrderRequestDto): ShopOrderResponseDto

    /** Same endpoint, subscription branch: `plan_id` instead of `product_id`. */
    @POST(ApiPaths.PAYMENT_INITIATE)
    suspend fun initiatePlanOrder(@Body body: PlanOrderRequestDto): ShopOrderResponseDto

    /** Settles the order using the `cb_token` from the gateway redirect. */
    @POST(ApiPaths.PAYMENT_VERIFY)
    suspend fun verifyPayment(@Body body: VerifyPaymentRequestDto): VerifyPaymentResponseDto

    // ── Playback ────────────────────────────────────────────────────────────

    /**
     * Step 1 of the handshake: the access decision, plus a 15-minute ticket.
     *
     * 403 carries the Persian reason the user may not watch, and that text is
     * shown verbatim. 429 when the ticket quota (30/hour) is exhausted.
     */
    @POST(ApiPaths.PLAYBACK_TICKET_TEMPLATE)
    suspend fun requestPlaybackTicket(@Path("lessonId") lessonId: String): PlaybackTicketDto

    /**
     * Step 2: exchange the ticket for the media URL(s).
     *
     * Fails closed if the ticket does not match both the user and the lesson.
     */
    @POST(ApiPaths.PLAYBACK_TEMPLATE)
    suspend fun resolvePlayback(
        @Path("lessonId") lessonId: String,
        @Body body: PlaybackRequestDto,
    ): PlaybackDto

    /**
     * Resume point. 404 on a server without the v1.8.0 patch — callers treat
     * that as "start from the beginning".
     */
    @GET(ApiPaths.LESSON_PROGRESS_TEMPLATE)
    suspend fun getLessonProgress(@Path("lessonId") lessonId: String): LessonProgressDto

    @PUT(ApiPaths.LESSON_PROGRESS_TEMPLATE)
    suspend fun saveLessonProgress(
        @Path("lessonId") lessonId: String,
        @Body body: LessonProgressRequest,
    ): LessonProgressDto
}
