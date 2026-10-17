package com.mrdastbaaf.mdbapp.features.auth.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import com.mrdastbaaf.mdbapp.features.auth.domain.model.SignupName
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserRole
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.mockk
import io.mockk.verify
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
 * Repository-level tests for the auth flow, against a real HTTP stack.
 *
 * `SessionManager` is mocked rather than real: it is backed by the Android
 * Keystore, which does not exist in a JVM unit test. Mocking it also makes the
 * assertion sharper — the question worth asking is not "did the token reach
 * storage" (that is `SecureTokenStore`'s job) but "did the repository persist it
 * at the right moment", which `verify { }` answers directly.
 */
class AuthRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var sessionManager: SessionManager
    private lateinit var currentUserStore: CurrentUserStore
    private lateinit var repository: AuthRepositoryImpl

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        sessionManager = mockk(relaxed = true)
        currentUserStore = CurrentUserStore()

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MdbApi::class.java)

        repository = AuthRepositoryImpl(api, json, sessionManager, currentUserStore)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── request-otp ─────────────────────────────────────────────────────────

    @Test
    fun `reports an existing account`() = runTest {
        server.enqueue(jsonResponse(200, """{ "success": true, "isRegistered": true, "expires_in_seconds": 120 }"""))

        val result = repository.requestOtp("09123456789")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isRegistered)
        assertEquals(120, result.getOrThrow().expiresInSeconds)
    }

    @Test
    fun `reports a new account`() = runTest {
        server.enqueue(jsonResponse(200, """{ "success": true, "isRegistered": false }"""))

        val result = repository.requestOtp("09123456789")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().isRegistered)
    }

    @Test
    fun `treats a missing isRegistered flag as a new account`() = runTest {
        // Safer default: asking a returning user for their name is a small
        // annoyance, whereas skipping it for a new account leaves them named
        // "هنرجو 1234" permanently.
        server.enqueue(jsonResponse(200, """{ "success": true }"""))

        val result = repository.requestOtp("09123456789")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().isRegistered)
    }

    @Test
    fun `surfaces the sms warning when the provider is not configured`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{ "success": true, "isRegistered": true, "smsWarning": "سرویس پیامک هنوز تنظیم نشده است." }""",
            ),
        )

        val result = repository.requestOtp("09123456789")

        assertEquals("سرویس پیامک هنوز تنظیم نشده است.", result.getOrThrow().smsWarning)
    }

    @Test
    fun `maps the rate limit response`() = runTest {
        server.enqueue(
            jsonResponse(
                429,
                """{ "code": "rate_limited", "message": "لطفاً ۲ دقیقه صبر کرده و مجدداً تلاش فرمایید." }""",
            ),
        )

        val result = repository.requestOtp("09123456789")

        val failure = result.exceptionOrNull()
        assertTrue(failure is Failure.RateLimited)
        assertEquals("لطفاً ۲ دقیقه صبر کرده و مجدداً تلاش فرمایید.", (failure as Failure).userMessage)
    }

    @Test
    fun `maps the privileged role rejection`() = runTest {
        server.enqueue(
            jsonResponse(
                403,
                """{ "code": "role_not_allowed", "message": "ورود با این شماره به اپ هنرجو مجاز نیست." }""",
            ),
        )

        val result = repository.requestOtp("09123456789")

        assertTrue(result.exceptionOrNull() is Failure.Forbidden)
    }

    // ── verify-otp ──────────────────────────────────────────────────────────

    @Test
    fun `stores the token before returning the profile`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "success": true,
                  "token": "42.9xK2mQsecret",
                  "user": {
                    "id": "42",
                    "phoneNumber": "09123456789",
                    "maskedPhone": "0912***6789",
                    "fullName": "محمدجواد فلان",
                    "role": "student",
                    "membership": null
                  }
                }
                """.trimIndent(),
            ),
        )

        val result = repository.verifyOtp("09123456789", "123456", name = null)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { sessionManager.onSignedIn("42.9xK2mQsecret") }
        assertEquals("محمدجواد فلان", result.getOrThrow().fullName)
        assertEquals(UserRole.STUDENT, result.getOrThrow().role)
        assertNull(result.getOrThrow().membership)
    }

    @Test
    fun `maps an active membership`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "success": true,
                  "token": "42.abc",
                  "user": {
                    "id": "42",
                    "phoneNumber": "09123456789",
                    "maskedPhone": "0912***6789",
                    "fullName": "محمدجواد فلان",
                    "role": "student",
                    "membership": {
                      "tier": "gold",
                      "planId": "gold_4821",
                      "status": "active",
                      "startedAt": "2026-01-01 10:00:00",
                      "expiresAt": "2027-01-01 10:00:00",
                      "formattedExpireDateJalali": "1405/10/11",
                      "daysRemaining": 77
                    }
                  }
                }
                """.trimIndent(),
            ),
        )

        val result = repository.verifyOtp("09123456789", "123456", name = null)

        val membership = result.getOrThrow().membership
        assertEquals(MembershipTier.GOLD, membership?.tier)
        assertEquals(77, membership?.daysRemaining)
        // The Jalali string comes from the server and is used verbatim — the
        // bridge owns the timezone handling, so re-deriving it would be wrong.
        assertEquals("1405/10/11", membership?.formattedExpireDateJalali)
    }

    @Test
    fun `sends a sanitised persian name for a new account`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{ "success": true, "token": "42.abc", "user": { "id": "42", "fullName": "علی رضایی" } }""",
            ),
        )

        repository.verifyOtp(
            phone = "09123456789",
            code = "123456",
            name = SignupName(firstName = "Ali رضایی", lastName = "  رضایی  "),
        )

        val body = server.takeRequest().body.readUtf8()
        // The Latin part is stripped client-side, matching what the server would
        // have done silently — so what the user sees is what the account stores.
        assertTrue(body.contains("\"first_name\":\"رضایی\""))
        assertTrue(body.contains("\"last_name\":\"رضایی\""))
    }

    @Test
    fun `omits the name keys entirely for an existing account`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{ "success": true, "token": "42.abc", "user": { "id": "42", "fullName": "علی" } }""",
            ),
        )

        repository.verifyOtp("09123456789", "123456", name = null)

        val body = server.takeRequest().body.readUtf8()
        // `explicitNulls = false` keeps these out of the body, so the plugin's
        // get_param() sees them as absent rather than as null.
        assertFalse(body.contains("first_name"))
        assertFalse(body.contains("last_name"))
    }

    @Test
    fun `passes through the remaining attempts on a wrong code`() = runTest {
        server.enqueue(
            jsonResponse(
                400,
                """
                {
                  "code": "invalid_otp",
                  "message": "کد تایید وارد شده نامعتبر یا منقضی شده است. تلاش باقی‌مانده: 3"
                }
                """.trimIndent(),
            ),
        )

        val result = repository.verifyOtp("09123456789", "000000", name = null)

        val failure = result.exceptionOrNull()
        assertTrue(failure is Failure.Validation)
        assertTrue((failure as Failure).userMessage?.contains("تلاش باقی‌مانده") == true)
        // A wrong code must never be treated as a dead session.
        assertFalse(failure is Failure.Unauthorized)
    }

    @Test
    fun `rejects a success response that carries no token`() = runTest {
        server.enqueue(jsonResponse(200, """{ "success": true, "user": { "id": "42" } }"""))

        val result = repository.verifyOtp("09123456789", "123456", name = null)

        assertTrue(result.exceptionOrNull() is Failure.InvalidResponse)
        verify(exactly = 0) { sessionManager.onSignedIn(any()) }
    }

    @Test
    fun `rejects a success response that carries no user`() = runTest {
        server.enqueue(jsonResponse(200, """{ "success": true, "token": "42.abc" }"""))

        val result = repository.verifyOtp("09123456789", "123456", name = null)

        assertTrue(result.exceptionOrNull() is Failure.InvalidResponse)
        // The payload is validated before anything is persisted, so a malformed
        // response cannot leave the app "signed in" with no identity to show.
        verify(exactly = 0) { sessionManager.onSignedIn(any()) }
    }

    // ── logout ──────────────────────────────────────────────────────────────

    @Test
    fun `clears the session after a successful logout`() = runTest {
        server.enqueue(jsonResponse(200, """{ "success": true }"""))

        repository.logout()

        verify(exactly = 1) { sessionManager.onSignedOut() }
    }

    @Test
    fun `clears the session even when the logout call fails`() = runTest {
        // A user who asked to sign out must end up signed out. The server-side
        // revocation is best effort; the local one is not.
        server.enqueue(MockResponse().setResponseCode(500))

        repository.logout()

        verify(exactly = 1) { sessionManager.onSignedOut() }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun jsonResponse(code: Int, body: String): MockResponse = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
