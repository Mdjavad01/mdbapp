package com.mrdastbaaf.mdbapp.features.appcontent.domain.model

/**
 * The hero block, as the site's editor configured it.
 *
 * Every field is optional because the panel lets the owner leave a stat or a
 * CTA blank; the screen drops what it does not get rather than rendering empty
 * boxes.
 */
data class Hero(
    val badgeText: String,
    val headline: String,
    val subheadline: String,
    val ctaPrimaryText: String,
    val ctaSecondaryText: String,
    val stats: List<HeroStat>,
)

data class HeroStat(val value: String, val label: String)

/**
 * The home screen's server-driven shape.
 *
 * Only what the app actually renders is kept: [featuredTitle] comes from the
 * `featured_courses` section, and [featuredCourseIds] is resolved against the
 * catalogue by the ViewModel.
 */
data class HomeConfig(
    val hero: Hero?,
    val featuredTitle: String?,
    val featuredSubtitle: String?,
    val featuredCourseIds: List<Int>,
    /**
     * The panel's bottom-bar switches.
     *
     * These describe the **web app's** dock (`home`, `catalogue`, `membership`,
     * `dashboard`) — there is no key for the shop, and none for a tab the
     * Android app has but the site does not. So this is read as a set of
     * "turned off" signals for the tabs that do correspond, never as a
     * replacement for the app's own tab list: following it literally would
     * delete the shop tab, which the web dock has never had.
     */
    val dock: List<AppMenuItem> = emptyList(),
    /** The `physical_products` section switch — the app's only shop toggle. */
    val shopEnabled: Boolean = true,
    /**
     * v1.7.3 — the brand palette the panel states, as raw hex.
     *
     * Null when the bridge is older than 1.7.22 or sent nothing usable; the
     * theme then keeps the anchors compiled into [com.mrdastbaaf.mdbapp.core.ui.theme.BrandPalette].
     */
    val theme: AppTheme? = null,
)

/** The panel's four brand anchors, unparsed. `null` per field = "use the default". */
data class AppTheme(
    val primary: String?,
    val accent: String?,
    val canvas: String?,
    val success: String?,
)

data class AppMenuItem(
    val key: String,
    val label: String,
    val enabled: Boolean,
)

/** A page in the app's guide list. */
data class AppPage(
    val id: String,
    val title: String,
    /** `all | members | admins` — enforced by the server, shown nowhere. */
    val access: String,
)

data class AppPageContent(
    val id: String,
    val title: String,
    /** Already stripped to plain text; the viewer does not render HTML. */
    val body: String,
    val updatedAtJalali: String,
)
