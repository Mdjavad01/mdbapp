package com.mrdastbaaf.mdbapp.features.catalogue.domain.model

import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier

/**
 * A course, as the catalogue shows it.
 *
 * Deliberately free of wire concerns: no nullable title, no `""` image URL. The
 * mapping in the data layer decides what a missing field degrades to, so no
 * screen has to ask "could this be null?".
 */
data class Course(
    /** Product `post_name`. Stable, used in URLs, may be Persian. */
    val id: String,
    val productId: Int?,
    val title: String,
    /** `post_excerpt` — often empty. */
    val description: String,
    /** `null` when the product has no thumbnail, so the UI can draw a placeholder. */
    val imageUrl: String?,
    val category: String?,
    val level: String?,
    val lessonsCount: Int,
    val durationMinutes: Int,
    /** Integer Toman. `0` means the course is free. */
    val priceToman: Long,
    val freePreviewCount: Int,
    /** Resolved server-side from paid orders. Never computed on the client. */
    val owned: Boolean,
)

/** A course plus its lesson list — the detail route only. */
data class CourseDetail(
    val course: Course,
    val lessons: List<Lesson>,
) {
    /**
     * The list route reports the real count; the detail route hard-codes
     * `lessonsCount: 0` (a server bug). Once the lessons are here, they are the
     * only trustworthy source.
     */
    val lessonCount: Int get() = lessons.size
}

/**
 * A lesson row.
 *
 * There is no video URL here and there never will be: the public lesson
 * selector does not return one. Playback URLs only exist inside the two-step
 * ticket flow.
 */
data class Lesson(
    val id: String,
    val title: String,
    val isFreePreview: Boolean,
    /** Minimum tier that unlocks it. [MembershipTier.FREE] when absent/unknown. */
    val requiredTier: MembershipTier,
    /** `null` when the server reports zero or nothing. */
    val durationMinutes: Int?,
)
