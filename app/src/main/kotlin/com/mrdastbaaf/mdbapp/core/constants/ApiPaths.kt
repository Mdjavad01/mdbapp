package com.mrdastbaaf.mdbapp.core.constants

/**
 * Every REST path in one place.
 *
 * Paths are relative to [com.mrdastbaaf.mdbapp.core.config.AppConfig.baseUrl],
 * which already includes the `/wp-json/mrdastbaaf/v1/` namespace. Retrofit
 * resolves them relative to that base, so none of these start with a slash.
 *
 * Source of truth: the `includes/` directory of
 * `wordpress-plugin/mrdastbaaf-bridge` in the `mrdb` repository. Do not add a
 * path here that the plugin does not register.
 */
object ApiPaths {

    // ── Public / diagnostics ────────────────────────────────────────────────
    const val VERSION = "version"

    // ── Authentication ──────────────────────────────────────────────────────
    const val AUTH_REQUEST_OTP = "auth/request-otp"
    const val AUTH_VERIFY_OTP = "auth/verify-otp"
    const val AUTH_ME = "auth/me"
    const val AUTH_LOGOUT = "auth/logout"

    // ── Courses ─────────────────────────────────────────────────────────────
    const val COURSES = "courses"
    const val MY_COURSES = "my/courses"

    /**
     * The **only** detail route the app uses.
     *
     * `courses/{slug}` exists on the server but is unusable for this catalogue.
     * Every product `post_name` is Persian, and the plugin's slug lookup strips
     * non-ASCII before searching — so it ends up looking for the literal `---`
     * and answers 404 with `tried: ["slug=---"]`. Verified against production:
     * `/courses/{slug}` 404s for every course, `/courses/by-id/{id}` returns 200.
     *
     * The numeric id is also the more stable key: it survives a title change,
     * which a slug does not.
     */
    const val COURSE_BY_ID_TEMPLATE = "courses/by-id/{productId}"

    // ── Membership ──────────────────────────────────────────────────────────
    const val MEMBERSHIP_PLANS = "memberships/plans"
    const val MEMBERSHIP_MY_STATUS = "memberships/my-status"

    // ── Progress / orders ───────────────────────────────────────────────────
    const val MY_PROGRESS = "me/progress"
    const val MY_ORDERS = "me/orders"

    // ── Playback ────────────────────────────────────────────────────────────
    /** Step 1: server-side access check; returns a 15-minute ticket plus the watermark identity. */
    const val PLAYBACK_TICKET_TEMPLATE = "lessons/{lessonId}/playback-ticket"

    /** Step 2: exchanges a valid ticket for the video URL(s). */
    const val PLAYBACK_TEMPLATE = "lessons/{lessonId}/playback"

    /**
     * Playback position. Added by the v1.8.0 server patch, so it 404s on a
     * server that has not taken the patch — the player treats that as "no
     * resume point" rather than an error.
     */
    const val LESSON_PROGRESS_TEMPLATE = "lessons/{lessonId}/progress"

    // ── Content ─────────────────────────────────────────────────────────────
    const val APP_UI = "app-ui"
    const val APP_PAGES = "app-pages"

    fun appPageContent(pageId: String): String = "app-pages/$pageId/content"

    // ── Shop (read-only showcase) ───────────────────────────────────────────
    const val SHOP_PRODUCTS = "shop/products"

    /** Server-driven home: hero copy, section order, featured ids. Public. */
    const val APP_UI = "app-ui"

    /** Editorial pages the site publishes to the app. Bare array. */
    const val APP_PAGES = "app-pages"

    /** Rendered `the_content` for one page. Access-filtered server-side. */
    const val APP_PAGE_CONTENT_TEMPLATE = "app-pages/{pageId}/content"

    /**
     * Creates an order and returns the gateway URL. Bearer required.
     *
     * Accepts `plan_id`, or `product_id` + `kind=shop` + the shipping fields.
     * The app never sends a card number: the gateway page lives in a WebView and
     * the server talks to Zibal directly.
     */
    const val PAYMENT_INITIATE = "payment/zibal/initiate"

    /**
     * Settles a payment. Public but protected by the `cb_token` that only the
     * gateway redirect carries — which is why the app has to intercept that
     * redirect rather than calling this on its own.
     */
    const val PAYMENT_VERIFY = "payment/zibal/verify"
}
