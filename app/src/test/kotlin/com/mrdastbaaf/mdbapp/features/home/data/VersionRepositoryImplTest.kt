package com.mrdastbaaf.mdbapp.features.home.data

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Repository-level tests, run against a real HTTP stack.
 *
 * [MockWebServer] rather than a mocked Retrofit interface on purpose: the
 * behaviour worth testing here lives in the serialisation, the error-envelope
 * parsing and the transport mapping — none of which a mocked interface would
 * exercise. A mocked interface would only assert that the repository calls the
 * method it obviously calls.
 */
class VersionRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: VersionRepositoryImpl

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

        repository = VersionRepositoryImpl(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `maps a healthy version response into the domain model`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "pwa_version": "1.5.6",
                      "plugin_version": "1.7.13",
                      "db_version": "1.5.1",
                      "server_time": "2026-10-16 12:00:00",
                      "timestamp": 1760625600,
                      "content_hash": "abc123"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.fetchServerInfo()

        assertTrue(result.isSuccess)
        val info = result.getOrThrow()
        assertEquals("1.7.13", info.pluginVersion)
        assertEquals("1.5.6", info.pwaVersion)
        assertEquals("1.5.1", info.databaseVersion)
        assertEquals(1760625600L, info.serverEpochSeconds)
        assertTrue(info.isHealthy)
    }

    @Test
    fun `tolerates unknown fields the server may add later`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "plugin_version": "1.8.0",
                      "something_new": { "nested": true }
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.fetchServerInfo()

        assertTrue(result.isSuccess)
        assertEquals("1.8.0", result.getOrThrow().pluginVersion)
    }

    @Test
    fun `substitutes a placeholder for missing fields rather than failing`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{ "plugin_version": "1.7.13" }"""),
        )

        val result = repository.fetchServerInfo()

        assertTrue(result.isSuccess)
        assertEquals("—", result.getOrThrow().pwaVersion)
    }

    @Test
    fun `treats an empty 200 as an invalid response`() = runTest {
        // A 200 with no version fields means we reached something that is not the
        // bridge — a cached error page, or the WAF answering on its behalf.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"),
        )

        val result = repository.fetchServerInfo()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Failure.InvalidResponse)
    }

    @Test
    fun `maps the bridge error envelope onto a typed failure`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "code": "forbidden",
                      "message": "مشاهده این جلسه نیازمند خرید یا تمدید اشتراک معتبر است.",
                      "data": { "status": 403 }
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.fetchServerInfo()

        assertTrue(result.isFailure)
        val failure = result.exceptionOrNull()
        assertTrue(failure is Failure.Forbidden)
        assertEquals("forbidden", (failure as Failure.Forbidden).code)
        // The server's Persian message is passed through untouched.
        assertEquals(
            "مشاهده این جلسه نیازمند خرید یا تمدید اشتراک معتبر است.",
            failure.userMessage,
        )
    }

    @Test
    fun `maps a 429 onto a rate limited failure`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody("""{ "code": "rate_limited", "message": "کمی بعد دوباره تلاش کنید." }"""),
        )

        val result = repository.fetchServerInfo()

        assertTrue(result.exceptionOrNull() is Failure.RateLimited)
    }

    @Test
    fun `falls back to the http status when the error body is not the expected envelope`() = runTest {
        // A gateway 502 with an HTML body: parsing fails, but the status must
        // still drive the failure rather than the parse error masking it.
        server.enqueue(
            MockResponse()
                .setResponseCode(502)
                .setHeader("Content-Type", "text/html")
                .setBody("<html><body>Bad Gateway</body></html>"),
        )

        val result = repository.fetchServerInfo()

        val failure = result.exceptionOrNull()
        assertTrue(failure is Failure.Server)
        assertEquals(502, (failure as Failure.Server).httpStatus)
    }

    @Test
    fun `a server failure is retryable and a validation failure is not`() = runTest {
        assertTrue(Failure.Server(httpStatus = 503).isRetryable)
        assertTrue(Failure.NoConnection().isRetryable)
        assertTrue(Failure.Timeout().isRetryable)
        assertTrue(!Failure.Validation().isRetryable)
        assertTrue(!Failure.Forbidden().isRetryable)
    }
}
