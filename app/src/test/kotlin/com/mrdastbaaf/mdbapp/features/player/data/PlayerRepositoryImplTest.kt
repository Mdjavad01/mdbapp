package com.mrdastbaaf.mdbapp.features.player.data

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * The playback handshake, against a real HTTP stack.
 *
 * The cases that matter are the ones the review called out as easy to get wrong:
 * the server lies about `streamType`, a refused lesson must surface the server's
 * own Persian message, and a server without the progress patch must not break
 * playback.
 */
class PlayerRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: PlayerRepositoryImpl

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

        repository = PlayerRepositoryImpl(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── The handshake ───────────────────────────────────────────────────────

    @Test
    fun `exchanges the ticket for a source`() = runTest {
        server.enqueue(ticketResponse())
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "videoUrl": "https://dll.mrdastbaaf.com/FA/lesson-01.mp4",
                  "watermarkText": "0912***4567 | ID: 42",
                  "expiresAt": "2026-10-16T08:15:00+00:00",
                  "qualities": [
                    { "label": "720p", "url": "https://dll.mrdastbaaf.com/FA/lesson-01-720.mp4" }
                  ],
                  "proxyMode": false
                }
                """.trimIndent(),
            ),
        )

        val session = repository.openLesson("12").getOrThrow()

        assertEquals("https://dll.mrdastbaaf.com/FA/lesson-01.mp4", session.videoUrl)
        assertEquals("0912***4567 | ID: 42", session.watermarkText)
        assertEquals(1, session.qualities.size)
        assertFalse(session.isHls)
    }

    @Test
    fun `sends the ticket id in the second request`() = runTest {
        server.enqueue(ticketResponse())
        server.enqueue(jsonResponse(200, """{ "videoUrl": "https://dll.mrdastbaaf.com/a.mp4" }"""))

        repository.openLesson("12")

        assertEquals("/lessons/12/playback-ticket", server.takeRequest().path)
        val second = server.takeRequest()
        assertEquals("/lessons/12/playback", second.path)
        assertTrue(second.body.readUtf8().contains("tkt_TESTTICKET12345"))
    }

    @Test
    fun `a 403 surfaces the server's own Persian message`() = runTest {
        server.enqueue(
            jsonResponse(
                403,
                """{ "code": "forbidden", "message": "مشاهده این جلسه نیازمند خرید یا تمدید اشتراک معتبر است." }""",
            ),
        )

        val failure = repository.openLesson("12").exceptionOrNull()

        assertTrue(failure is Failure.Forbidden)
        assertEquals(
            "مشاهده این جلسه نیازمند خرید یا تمدید اشتراک معتبر است.",
            (failure as Failure).userMessage,
        )
    }

    @Test
    fun `a ticket without an id is rejected rather than sent as empty`() = runTest {
        server.enqueue(jsonResponse(200, """{ "lessonId": "12" }"""))

        val failure = repository.openLesson("12").exceptionOrNull()

        assertTrue(failure is Failure.InvalidResponse)
        // Only the ticket call happened; the second step must not be attempted.
        assertEquals(1, server.requestCount)
    }

    // ── HLS detection ───────────────────────────────────────────────────────

    @Test
    fun `detects HLS from the URL, not from streamType`() = runTest {
        // The current server hard-codes streamType "hls" while serving MP4.
        server.enqueue(ticketResponse(streamType = "hls"))
        server.enqueue(
            jsonResponse(200, """{ "videoUrl": "https://dll.mrdastbaaf.com/FA/lesson-01.mp4" }"""),
        )

        val session = repository.openLesson("12").getOrThrow()

        assertFalse("an .mp4 must not be treated as HLS", session.isHls)
    }

    @Test
    fun `treats an m3u8 as HLS even with a query string`() = runTest {
        server.enqueue(ticketResponse())
        server.enqueue(
            jsonResponse(
                200,
                """{ "videoUrl": "https://dll.mrdastbaaf.com/FA/lesson-01.m3u8?token=abc" }""",
            ),
        )

        assertTrue(repository.openLesson("12").getOrThrow().isHls)
    }

    @Test
    fun `prefers videoType when the patched server sends it`() = runTest {
        server.enqueue(ticketResponse())
        server.enqueue(
            jsonResponse(
                200,
                """{ "videoUrl": "https://dll.mrdastbaaf.com/FA/lesson-01", "videoType": "hls" }""",
            ),
        )

        assertTrue(repository.openLesson("12").getOrThrow().isHls)
    }

    @Test
    fun `refuses proxy mode instead of playing a stream that dies in a minute`() = runTest {
        server.enqueue(ticketResponse())
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "videoUrl": "https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/lessons/12/stream?token=XyZ",
                  "proxyMode": true
                }
                """.trimIndent(),
            ),
        )

        val failure = repository.openLesson("12").exceptionOrNull()

        assertTrue(failure is Failure.InvalidResponse)
    }

    // ── Progress ────────────────────────────────────────────────────────────

    @Test
    fun `reads the resume point`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{ "positionSeconds": 431, "durationSeconds": 1420, "completed": false }""",
            ),
        )

        val progress = repository.progress("12").getOrThrow()

        assertEquals(431, progress?.positionSeconds)
        assertEquals(1420, progress?.durationSeconds)
        assertEquals(431, progress?.resumePositionSeconds)
    }

    @Test
    fun `a 404 on progress means the server predates the patch, not an error`() = runTest {
        server.enqueue(jsonResponse(404, """{ "code": "not_found" }"""))

        val result = repository.progress("12")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `a completed lesson restarts from the beginning`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{ "positionSeconds": 1420, "durationSeconds": 1420, "completed": true }""",
            ),
        )

        assertEquals(0, repository.progress("12").getOrThrow()?.resumePositionSeconds)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun ticketResponse(streamType: String = "hls"): MockResponse = jsonResponse(
        200,
        """
        {
          "ticketId": "tkt_TESTTICKET12345",
          "lessonId": "12",
          "expiresInSeconds": 900,
          "watermarkText": "0912***4567 | ID: 42",
          "watermarkUserId": "42",
          "streamType": "$streamType"
        }
        """.trimIndent(),
    )

    private fun jsonResponse(code: Int, body: String): MockResponse = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
