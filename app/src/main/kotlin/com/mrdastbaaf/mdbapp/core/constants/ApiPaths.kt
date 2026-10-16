package com.mrdastbaaf.mdbapp.core.constants

/**
 * Every REST path in one place.
 *
 * Paths are relative to [com.mrdastbaaf.mdbapp.core.config.AppConfig.baseUrl],
 * which already includes the `/wp-json/mrdastbaaf/v1/` namespace. Retrofit
 * resolves them relative to that base, so none of these start with a slash.
 *
 * Source of truth: `wordpress-plugin/mrdastbaaf-bridge/includes/*.php` in the
 * `mrdb` repository. Do not add a path here that the plugin does not register.
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

    /** `GET courses/{slug}` — the slug is a product `post_name` and may contain Persian characters. */
    fun courseBySlug(slug: String): String = "courses/$slug"

    /** `GET courses/by-id/{productId}` — the unambiguous numeric route. */
    fun courseById(productId: Int): String = "courses/by-id/$productId"

    // ── Membership ──────────────────────────────────────────────────────────
    const val MEMBERSHIP_PLANS = "memberships/plans"
    const val MEMBERSHIP_MY_STATUS = "memberships/my-status"

    // ── Progress / orders ───────────────────────────────────────────────────
    const val MY_PROGRESS = "me/progress"
    const val MY_ORDERS = "me/orders"

    // ── Playback ────────────────────────────────────────────────────────────
    /** Step 1: server-side access check; returns a 15-minute ticket plus the watermark identity. */
    fun playbackTicket(lessonId: String): String = "lessons/$lessonId/playback-ticket"

    /** Step 2: exchanges a valid ticket for the video URL(s). */
    fun playback(lessonId: String): String = "lessons/$lessonId/playback"

    // ── Content ─────────────────────────────────────────────────────────────
    const val APP_UI = "app-ui"
    const val APP_PAGES = "app-pages"

    fun appPageContent(pageId: String): String = "app-pages/$pageId/content"

    // ── Shop (read-only showcase) ───────────────────────────────────────────
    const val SHOP_PRODUCTS = "shop/products"
}
