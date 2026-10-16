package com.mrdastbaaf.mdbapp.core.navigation

/**
 * Every navigation destination, in one place.
 *
 * Routes are plain strings rather than a serialisable type because the app needs
 * deep links later (the payment flow returns to the app through a URL), and a
 * string route keeps the manifest, the NavHost and the hand-off in agreement.
 *
 * Only [HOME] is implemented in phase P0. The rest are declared now so that the
 * shape of the graph is decided once — adding a screen later means adding a
 * `composable(...)` block, not rethinking the navigation model.
 */
object Routes {

    const val HOME = "home"

    // ── Auth ────────────────────────────────────────────────────────────────
    const val LOGIN = "login"
    const val OTP = "otp"
    const val SIGNUP_DETAILS = "signup"

    // ── Catalogue ───────────────────────────────────────────────────────────
    const val CATALOGUE = "catalogue"
    const val MY_COURSES = "my-courses"

    /** `course/{slug}` — the slug is the product `post_name` and may be Persian. */
    const val COURSE_DETAIL = "course/{slug}"
    fun courseDetail(slug: String): String = "course/${encode(slug)}"

    // ── Player ──────────────────────────────────────────────────────────────
    /** `player/{lessonId}?courseSlug=…` — the slug is carried for the back stack. */
    const val PLAYER = "player/{lessonId}?courseSlug={courseSlug}"
    fun player(lessonId: String, courseSlug: String? = null): String =
        "player/${encode(lessonId)}?courseSlug=${encode(courseSlug.orEmpty())}"

    // ── Membership & account ────────────────────────────────────────────────
    const val MEMBERSHIP = "membership"
    const val DASHBOARD = "dashboard"
    const val ORDERS = "orders"
    const val PROFILE = "profile"

    // ── Content ─────────────────────────────────────────────────────────────
    const val SHOP = "shop"

    /** `page/{id}` — an editorial page served by `GET /app-pages`. */
    const val APP_PAGE = "page/{pageId}"
    fun appPage(pageId: String): String = "page/${encode(pageId)}"

    /**
     * Percent-encodes a path segment.
     *
     * Course slugs and lesson ids are ASCII in practice, but app-page ids and any
     * future Persian slug are not guaranteed to be, and a raw Persian character
     * in a route would break `NavController.navigate`.
     */
    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
