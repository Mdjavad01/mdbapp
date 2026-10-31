package com.mrdastbaaf.mdbapp.features.appcontent.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * The home configuration and the guide pages.
 *
 * The case that matters is the empty headline: the panel lets the owner clear
 * the hero, and a home screen that renders a blank gradient block looks broken
 * rather than empty.
 */
class AppContentRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AppContentRepositoryImpl

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

        repository = AppContentRepositoryImpl(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `maps the hero and keeps only the stats that have content`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "hero": {
                    "badgeText": "دستباف ایرانی",
                    "headline": "هنر دست، <b>در خانهٔ تو</b>",
                    "subheadline": "آموزش گلیم و تابلوفرش",
                    "ctaPrimaryText": "شروع کن",
                    "ctaSecondaryText": "",
                    "stat1Value": "۱۲۰۰",
                    "stat1Label": "هنرجو",
                    "stat2Value": "",
                    "stat2Label": ""
                  },
                  "sections": [
                    { "key": "featured_courses", "title": "منتخب", "subtitle": "برای شروع", "enabled": true }
                  ],
                  "featuredCourseIds": [123, 0, 456]
                }
                """.trimIndent(),
            ),
        )

        val config = repository.home().getOrThrow()

        assertEquals("هنر دست، در خانهٔ تو", config.hero?.headline)
        assertEquals("دستباف ایرانی", config.hero?.badgeText)
        // The blank second stat is dropped rather than drawn as an empty column.
        assertEquals(1, config.hero?.stats?.size)
        assertEquals("۱۲۰۰", config.hero?.stats?.first()?.value)
        assertEquals("منتخب", config.featuredTitle)
        assertEquals(listOf(123, 456), config.featuredCourseIds)
    }

    @Test
    fun `a hero without a headline fails instead of drawing an empty block`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{ "hero": { "headline": "   ", "subheadline": "…" }, "featuredCourseIds": [] }""",
            ),
        )

        val failure = repository.home().exceptionOrNull()

        assertTrue(failure is Failure.InvalidResponse)
    }

    @Test
    fun `parses the pages array and drops entries without an id`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                [
                  { "id": "about", "title": "درباره ما", "access": "all" },
                  { "title": "بدون شناسه", "access": "all" },
                  { "id": "faq", "title": "سؤالات <i>متداول</i>", "access": "members" }
                ]
                """.trimIndent(),
            ),
        )

        val pages = repository.pages().getOrThrow()

        assertEquals(2, pages.size)
        assertEquals("about", pages[0].id)
        assertEquals("سؤالات متداول", pages[1].title)
        assertEquals("members", pages[1].access)
    }

    @Test
    fun `strips the html from a page body`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "id": "about",
                  "title": "درباره ما",
                  "html": "<p style=\"text-align: right\">ما یک <b>کارگاه</b> هستیم.</p><p>از ۱۳۹۰.</p>",
                  "updatedAtJalali": "1405/07/24"
                }
                """.trimIndent(),
            ),
        )

        val page = repository.pageContent("about").getOrThrow()

        // HtmlText turns each block tag into one newline and trims the lines,
        // so blocks are separated by "\n", not by a blank line.
        assertEquals("ما یک کارگاه هستیم.\nاز ۱۳۹۰.", page.body)
        assertEquals("1405/07/24", page.updatedAtJalali)
    }

    @Test
    fun `reads the panel palette and keeps blanks as null`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "hero": { "headline": "هنر دست" },
                  "theme": {
                    "primary": " #123456 ",
                    "accent": "#ABCDEF",
                    "canvas": "   ",
                    "success": null
                  },
                  "featuredCourseIds": []
                }
                """.trimIndent(),
            ),
        )

        val theme = repository.home().getOrThrow().theme

        // Trimmed, but not validated here: parsing is the UI's job, and a value
        // the panel stored wrong has to reach BrandPalette to be rejected.
        assertEquals("#123456", theme?.primary)
        assertEquals("#ABCDEF", theme?.accent)
        // Blank and absent are the same thing: "use the default anchor".
        assertNull(theme?.canvas)
        assertNull(theme?.success)
    }

    @Test
    fun `an older bridge without a theme block leaves the palette alone`() = runTest {
        server.enqueue(
            jsonResponse(200, """{ "hero": { "headline": "هنر دست" }, "featuredCourseIds": [] }"""),
        )

        val config = repository.home().getOrThrow()

        // Null, not an empty AppTheme — the theme keeps its compiled anchors.
        assertNull(config.theme)
    }

    private fun jsonResponse(code: Int, body: String): MockResponse = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
