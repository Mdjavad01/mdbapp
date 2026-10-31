package com.mrdastbaaf.mdbapp.core.network.interceptor

import com.mrdastbaaf.mdbapp.core.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The app has to say that it is the app.
 *
 * The bridge records where each order came from; without this header a purchase
 * made in the app is filed as a website purchase, and the owner cannot tell the
 * two channels apart.
 */
class ClientInfoInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `every request carries the client header with the version`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(ClientInfoInterceptor())
            .build()

        client.newCall(
            Request.Builder().url(server.url("/app-ui")).build(),
        ).execute().close()

        val recorded = server.takeRequest()
        assertEquals(
            "android/${AppConfig.versionName}",
            recorded.getHeader("X-MDB-Client"),
        )
    }
}
