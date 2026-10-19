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
