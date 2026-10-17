package com.mrdastbaaf.mdbapp.features.catalogue.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Repository tests against a real HTTP stack.
 *
 * The mapping rules these pin are the ones that are easy to get wrong and
 * expensive to discover in production:
 *
 *  - a product without an id is dropped rather than given a fake one,
 *  - an unknown `requiredTier` becomes bronze (the bridge's own default), not
 *    free — otherwise a paid lesson would render unlocked,
 *  - the detail route's `lessonsCount` is ignored in favour of the actual list,
 *  - `GET /courses` is a bare array while `GET /my/courses` is enveloped.
 */
class CatalogueRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: CatalogueRepositoryImpl

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MdbApi::class.java)

        repository = CatalogueRepositoryImpl(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── GET /courses ────────────────────────────────────────────────────────

    @Test
    fun `parses the bare array the list route returns`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                [
                  {
                    "id": "gelim-bafi",
                    "productId": 123,
                    "title": "گلیم‌بافی مقدماتی",
                    "description": "دورهٔ آغازین",
                    "image": "https://cdn.example/x-300x300.jpg",
                    "category": "دوره‌های آموزشی",
                    "level": "مقدماتی",
                    "lessonsCount": 24,
                    "duration": 480,
                    "price": 1850000,
                    "freePreviewCount": 3,
                    "owned": true
                  }
                ]
                """.trimIndent(),
            ),
        )

        val result = repository.getCourses()

        val courses = result.getOrThrow()
        assertEquals(1, courses.size)
        val course = courses.single()
        assertEquals("gelim-bafi", course.id)
        assertEquals(123, course.productId)
        assertEquals("گلیم‌بافی مقدماتی", course.title)
        assertEquals("https://cdn.example/x-300x300.jpg", course.imageUrl)
        assertEquals(24, course.lessonsCount)
        assertEquals(480, course.durationMinutes)
        assertEquals(1_850_000L, course.priceToman)
        assertTrue(course.owned)
    }

    @Test
    fun `drops a product that has no id instead of inventing one`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                [
                  { "title": "بدون شناسه" },
                  { "id": "ok", "title": "سالم" }
                ]
                """.trimIndent(),
            ),
        )

        val courses = repository.getCourses().getOrThrow()

        assertEquals(1, courses.size)
        assertEquals("ok", courses.single().id)
    }

    @Test
    fun `degrades a missing thumbnail and a missing price to safe values`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """[{ "id": "free-course", "title": "رایگان", "image": "", "price": 0 }]""",
            ),
        )

        val course = repository.getCourses().getOrThrow().single()

        assertNull(course.imageUrl)
        assertEquals(0L, course.priceToman)
        assertEquals(0, course.lessonsCount)
        assertFalse(course.owned)
    }

    // ── GET /courses/{slug} ─────────────────────────────────────────────────

    @Test
    fun `reads lessons and ignores the broken lessonsCount on the detail route`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "id": "gelim-bafi",
                  "productId": 123,
                  "title": "گلیم‌بافی مقدماتی",
                  "lessonsCount": 0,
                  "duration": 480,
                  "price": 1850000,
                  "owned": false,
                  "lessons": [
                    { "id": "1", "title": "جلسهٔ اول", "isFreePreview": true, "requiredTier": "bronze", "duration": 22 },
                    { "id": "2", "title": "جلسهٔ دوم", "isFreePreview": false, "requiredTier": "gold", "duration": null }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val detail = repository.getCourse(123).getOrThrow()

        // The server sends 0 here; the list is the only trustworthy count.
        assertEquals(2, detail.lessonCount)
        assertEquals(480, detail.course.durationMinutes)
        assertTrue(detail.lessons.first().isFreePreview)
        assertEquals(MembershipTier.GOLD, detail.lessons.last().requiredTier)
        assertNull(detail.lessons.last().durationMinutes)
    }

    @Test
    fun `treats an unknown required tier as bronze, the server's own default`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "id": "x", "title": "دوره",
                  "lessons": [
                    { "id": "1", "title": "بدون سطح" },
                    { "id": "2", "title": "سطح ناشناس", "requiredTier": "platinum" }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val lessons = repository.getCourse(1).getOrThrow().lessons

        assertEquals(MembershipTier.BRONZE, lessons[0].requiredTier)
        assertEquals(MembershipTier.BRONZE, lessons[1].requiredTier)
    }

    @Test
    fun `uses the by id route, not the slug route`() = runTest {
        server.enqueue(jsonResponse(200, """{ "id": "x", "title": "دوره" }"""))

        repository.getCourse(12360)

        // The slug route 404s for every Persian post_name, so this path is the
        // contract: if it ever changes back, playback and detail break silently.
        assertEquals("/courses/by-id/12360", server.takeRequest().path)
    }

    @Test
    fun `decodes the percent encoded id the bridge returns`() = runTest {
        // The bridge hands back the post_name already URL-encoded.
        server.enqueue(
            jsonResponse(
                200,
                """[{ "id": "%D8%AF%D9%88%D8%B1%D9%87-%D8%AC%D8%AA", "title": "دوره" }]""",
            ),
        )

        val course = repository.getCourses().getOrThrow().single()

        assertEquals("دوره-جت", course.id)
    }

    @Test
    fun `maps a 404 onto NotFound`() = runTest {
        server.enqueue(
            jsonResponse(
                404,
                """{ "code": "not_found", "message": "دوره یافت نشد.", "data": { "status": 404 } }""",
            ),
        )

        val result = repository.getCourse(999)

        assertTrue(result.isFailure)
        val failure = result.exceptionOrNull()
        assertTrue(failure is Failure.NotFound)
        assertEquals("دوره یافت نشد.", (failure as Failure).userMessage)
    }

    // ── GET /my/courses ─────────────────────────────────────────────────────

    @Test
    fun `reads the envelope my courses uses`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "success": true,
                  "count": 1,
                  "courses": [
                    { "id": "owned-course", "title": "دورهٔ خریداری‌شده", "owned": true }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val courses = repository.getMyCourses().getOrThrow()

        assertEquals(1, courses.size)
        assertEquals("owned-course", courses.single().id)
        assertTrue(courses.single().owned)
    }

    @Test
    fun `maps a 401 onto Unauthorized so the session can be cleared`() = runTest {
        server.enqueue(
            jsonResponse(
                401,
                """{ "code": "unauthorized", "message": "لطفاً دوباره وارد شوید." }""",
            ),
        )

        val failure = repository.getMyCourses().exceptionOrNull()

        assertTrue(failure is Failure.Unauthorized)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun jsonResponse(code: Int, body: String): MockResponse = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
