package com.mrdastbaaf.mdbapp.features.catalogue.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.core.network.safeApiCall
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.CourseDetailDto
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.CourseDto
import com.mrdastbaaf.mdbapp.features.catalogue.data.remote.dto.LessonDto
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Course
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.CourseDetail
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Lesson
import com.mrdastbaaf.mdbapp.features.catalogue.domain.repository.CatalogueRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The catalogue, mapped from WooCommerce products onto the domain.
 *
 * Two mapping decisions are worth stating because they are not obvious:
 *
 *  - **A course with no id or no title is dropped, not defaulted.** The id is
 *    the product `post_name` and is the key for every later call; inventing one
 *    would produce a card that cannot be opened. Dropping it keeps the rest of
 *    the list usable, which matters because the bridge returns the whole
 *    catalogue in one response — one bad product must not blank the screen.
 *
 *  - **An unrecognised `requiredTier` becomes [MembershipTier.BRONZE].** The
 *    bridge's own fallback is `bronze` (`get_lesson_required_tier`), so a
 *    missing value means "the lowest paid tier", not "free". Defaulting to
 *    [MembershipTier.FREE] here would draw an unlocked icon on a paid lesson.
 */
@Singleton
class CatalogueRepositoryImpl @Inject constructor(
    private val api: MdbApi,
    private val json: Json,
) : CatalogueRepository {

    override suspend fun getCourses(): Result<List<Course>> =
        safeApiCall(json) { api.getCourses() }
            .mapCatching { dtos -> dtos.mapNotNull { it.toDomain() } }

    override suspend fun getCourse(slug: String): Result<CourseDetail> =
        safeApiCall(json) { api.getCourse(slug) }
            .mapCatching { dto ->
                dto.toDomain() ?: throw Failure.InvalidResponse(
                    "اطلاعات دوره از سرور ناقص بود.",
                )
            }

    override suspend fun getMyCourses(): Result<List<Course>> =
        safeApiCall(json) { api.getMyCourses() }
            .mapCatching { dto -> dto.courses.mapNotNull { it.toDomain() } }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private fun CourseDto.toDomain(): Course? = buildCourse(
        id = id,
        productId = productId,
        title = title,
        description = description,
        image = image,
        category = category,
        level = level,
        lessonsCount = lessonsCount,
        duration = duration,
        price = price,
        freePreviewCount = freePreviewCount,
        owned = owned,
    )

    private fun CourseDetailDto.toDomain(): CourseDetail? {
        val course = buildCourse(
            id = id,
            productId = productId,
            title = title,
            description = description,
            image = image,
            category = category,
            level = level,
            lessonsCount = lessonsCount,
            duration = duration,
            price = price,
            freePreviewCount = freePreviewCount,
            owned = owned,
        ) ?: return null

        return CourseDetail(
            course = course,
            lessons = lessons.mapNotNull { it.toDomain() },
        )
    }

    private fun LessonDto.toDomain(): Lesson? {
        val lessonId = id?.takeIf { it.isNotBlank() } ?: return null
        val lessonTitle = title?.takeIf { it.isNotBlank() } ?: return null

        return Lesson(
            id = lessonId,
            title = lessonTitle,
            isFreePreview = isFreePreview == true,
            requiredTier = tierOf(requiredTier),
            durationMinutes = duration?.takeIf { it > 0 },
        )
    }

    private fun buildCourse(
        id: String?,
        productId: Int?,
        title: String?,
        description: String?,
        image: String?,
        category: String?,
        level: String?,
        lessonsCount: Int?,
        duration: Int?,
        price: Long?,
        freePreviewCount: Int?,
        owned: Boolean?,
    ): Course? {
        val courseId = id?.takeIf { it.isNotBlank() } ?: return null
        val courseTitle = title?.takeIf { it.isNotBlank() } ?: return null

        return Course(
            id = courseId,
            productId = productId,
            title = courseTitle,
            description = description.orEmpty().trim(),
            imageUrl = image?.takeIf { it.isNotBlank() },
            category = category?.takeIf { it.isNotBlank() },
            level = level?.takeIf { it.isNotBlank() },
            lessonsCount = lessonsCount?.coerceAtLeast(0) ?: 0,
            durationMinutes = duration?.coerceAtLeast(0) ?: 0,
            priceToman = price?.coerceAtLeast(0) ?: 0,
            freePreviewCount = freePreviewCount?.coerceAtLeast(0) ?: 0,
            owned = owned == true,
        )
    }

    private fun tierOf(raw: String?): MembershipTier = when (raw?.trim()?.lowercase()) {
        "free" -> MembershipTier.FREE
        "silver" -> MembershipTier.SILVER
        "gold" -> MembershipTier.GOLD
        // `bronze` and anything unrecognised: the bridge's own default.
        else -> MembershipTier.BRONZE
    }
}
