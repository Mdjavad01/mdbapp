package com.mrdastbaaf.mdbapp.features.catalogue.domain.repository

import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Course
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.CourseDetail

/**
 * Read-only access to the course catalogue.
 *
 * Every method returns `Result` and carries a
 * [com.mrdastbaaf.mdbapp.core.error.Failure] on the error path, so no screen
 * ever sees an HTTP status.
 */
interface CatalogueRepository {

    /**
     * `GET /courses` — public, but the Bearer token is attached when present
     * because that is the only thing that makes `owned` truthful.
     */
    suspend fun getCourses(): Result<List<Course>>

    /** `GET /courses/{slug}` — the slug is the product `post_name`. */
    suspend fun getCourse(slug: String): Result<CourseDetail>

    /** `GET /my/courses` — Bearer required; every item comes back `owned = true`. */
    suspend fun getMyCourses(): Result<List<Course>>
}
