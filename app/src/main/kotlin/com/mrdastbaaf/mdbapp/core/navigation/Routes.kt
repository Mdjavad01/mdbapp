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

    /**
     * The signed-in shell: a bottom bar hosting the catalogue and the account
     * panel. The two tabs are *state* inside that screen, not routes — switching
     * tabs must not grow the back stack, and a course detail screen pushed on
     * top has to return to the tab the user was on.
     */
    const val MAIN = "main"

    // ── Auth ────────────────────────────────────────────────────────────────
    const val LOGIN = "login"

    /**
     * The OTP screen carries the phone number and whether the account already
     * exists. Both are needed to render the right form — an existing account
     * gets a 6-digit field, a new one also gets name fields — and re-deriving
     * them would mean asking the server a second time.
     *
     * `registered` travels as `1`/`0` rather than a boolean string: Navigation
     * Compose parses route arguments by type, and a lowercase `true`/`false`
     * round-trips through `NavType.BoolType` less predictably than a digit.
     */
    const val OTP = "otp?phone={phone}&registered={registered}"
    fun otp(phone: String, isRegistered: Boolean): String =
        "otp?phone=${encode(phone)}&registered=${if (isRegistered) 1 else 0}"

    const val SIGNUP_DETAILS = "signup"

    // ── Catalogue ───────────────────────────────────────────────────────────

    /**
     * `course/{productId}` — the numeric WooCommerce product id, not the slug.
     * The slug route 404s for Persian `post_name`s server-side; see
     * [com.mrdastbaaf.mdbapp.core.constants.ApiPaths.COURSE_BY_ID_TEMPLATE].
     */
    const val COURSE_DETAIL = "course/{productId}"
    fun courseDetail(productId: Int): String = "course/$productId"

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
