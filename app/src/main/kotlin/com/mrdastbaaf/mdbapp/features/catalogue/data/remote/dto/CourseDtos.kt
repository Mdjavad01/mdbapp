package com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One course as the bridge returns it in `GET /courses` and `GET /my/courses`.
 *
 * Every field is nullable and defaulted. The bridge builds this payload from
 * WooCommerce products, and a product can legitimately be missing a thumbnail,
 * a category or a level — a missing value must degrade, not crash the list.
 */
@Serializable
data class CourseDto(
    /** The product `post_name`. This is the id used in every URL. */
    val id: String? = null,
    val productId: Int? = null,
    val title: String? = null,
    val description: String? = null,
    /** WordPress `medium` size, or `""` when the product has no thumbnail. */
    val image: String? = null,
    /** First `product_cat` term *name*, not the slug. */
    val category: String? = null,
    val level: String? = null,
    val lessonsCount: Int? = null,
    /** Total minutes across all lessons. */
    val duration: Int? = null,
    /** Integer Toman. `0` means free. */
    val price: Long? = null,
    val freePreviewCount: Int? = null,
    /** `true` only when a valid Bearer token was attached. */
    val owned: Boolean? = null,
)

/**
 * `GET /courses/{slug}` — a bare object, not an envelope.
 *
 * It repeats the list fields and adds `lessons`. Note that `lessonsCount` is
 * hard-coded to `0` on this route (a server bug we reported); the app must use
 * `lessons.size` when it has the detail payload, and the list's count otherwise.
 */
@Serializable
data class CourseDetailDto(
    val id: String? = null,
    val productId: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val category: String? = null,
    val level: String? = null,
    val lessonsCount: Int? = null,
    val duration: Int? = null,
    val price: Long? = null,
    val freePreviewCount: Int? = null,
    val owned: Boolean? = null,
    val lessons: List<LessonDto> = emptyList(),
)

/**
 * A lesson inside a course.
 *
 * `video_url` is deliberately absent from the contract — the public lesson
 * selector never returns it. Playback URLs only exist inside a ticket flow.
 */
@Serializable
data class LessonDto(
    /** Numeric on the wire, cast to string by the bridge. */
    val id: String? = null,
    val title: String? = null,
    val isFreePreview: Boolean? = null,
    /** `"bronze" | "silver" | "gold"`. Unknown values must be tolerated. */
    val requiredTier: String? = null,
    /** Minutes, or `null` when zero/unknown. */
    val duration: Int? = null,
)

/** `GET /my/courses` — the one course route that *is* enveloped. */
@Serializable
data class MyCoursesDto(
    val success: Boolean? = null,
    val courses: List<CourseDto> = emptyList(),
    val count: Int? = null,
)
