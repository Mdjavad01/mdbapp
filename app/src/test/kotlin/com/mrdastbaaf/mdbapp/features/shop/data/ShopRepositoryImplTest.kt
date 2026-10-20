package com.mrdastbaaf.mdbapp.features.shop.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShippingDetails
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * The shop catalogue and the checkout handshake.
 *
 * The checkout cases matter most: the request field names are snake_case because
 * the bridge reads them with `get_param`, and a wrong name would create an order
 * with an empty address rather than fail loudly.
 */
class ShopRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ShopRepositoryImpl

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val shipping = ShippingDetails(
        fullName = "محمد جواد",
        phone = "09123456789",
        address = "تهران، خیابان ولیعصر، پلاک ۱۲",
        postalCode = "1234567890",
    )

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MdbApi::class.java)

        repository = ShopRepositoryImpl(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── Catalogue ───────────────────────────────────────────────────────────

    @Test
    fun `parses featured products and categories`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "success": true,
                  "featured": [
                    { "id": 71, "title": "گلیم دستباف", "price": 850000, "imageUrl": "https://cdn/a.jpg" }
                  ],
                  "categories": [
                    {
                      "id": 34, "slug": "handmade", "name": "دست‌ساز",
                      "products": [
                        { "id": 72, "title": "تابلو فرش", "price": 1200000, "imageUrl": null }
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val catalog = repository.catalog().getOrThrow()

        assertEquals(1, catalog.featured.size)
        assertEquals("گلیم دستباف", catalog.featured.first().title)
        assertEquals(850_000L, catalog.featured.first().priceToman)
        assertEquals(1, catalog.categories.size)
        assertEquals("دست‌ساز", catalog.categories.first().name)
        assertEquals(72, catalog.categories.first().products.first().id)
    }

    @Test
    fun `drops products without an id and categories left empty`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "featured": [],
                  "categories": [
                    { "id": 1, "name": "خالی", "products": [ { "title": "بدون شناسه" } ] },
                    { "id": 2, "name": "پر", "products": [ { "id": 5, "title": "محصول", "price": 10 } ] }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val catalog = repository.catalog().getOrThrow()

        assertEquals(1, catalog.categories.size)
        assertEquals(2, catalog.categories.first().id)
    }

    @Test
    fun `caches the catalogue until forced`() = runTest {
        server.enqueue(jsonResponse(200, """{ "featured": [], "categories": [] }"""))

        repository.catalog().getOrThrow()
        repository.catalog().getOrThrow()

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `finds a product from the cached catalogue`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{ "featured": [ { "id": 9, "title": "گلیم", "price": 100 } ], "categories": [] }""",
            ),
        )

        repository.catalog().getOrThrow()
        val product = repository.product(9)

        assertNotNull(product)
        assertEquals("گلیم", product?.title)
    }

    // ── Checkout ────────────────────────────────────────────────────────────

    @Test
    fun `creates an order with the snake case body the bridge expects`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "orderId": "ord_1",
                  "orderNumber": "MD-1001",
                  "trackId": 987654,
                  "paymentUrl": "https://gateway.zibal.ir/start/987654"
                }
                """.trimIndent(),
            ),
        )

        val session = repository.createOrder(71, shipping).getOrThrow()

        val request = server.takeRequest()
        assertEquals("/payment/zibal/initiate", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"product_id\":71"))
        assertTrue(body.contains("\"kind\":\"shop\""))
        assertTrue(body.contains("\"full_name\""))
        assertTrue(body.contains("\"postal_code\""))

        assertEquals("ord_1", session.orderId)
        assertEquals(987_654L, session.trackId)
        assertEquals("https://gateway.zibal.ir/start/987654", session.paymentUrl)
    }

    @Test
    fun `a subscription order sends plan_id and never product_id`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """
                {
                  "orderId": "ord_2",
                  "orderNumber": "MD-2002",
                  "trackId": 111222,
                  "paymentUrl": "https://gateway.zibal.ir/start/111222"
                }
                """.trimIndent(),
            ),
        )

        val session = repository.createPlanOrder("gold-12").getOrThrow()

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"plan_id\":\"gold-12\""))
        // The bridge branches on which id is present; sending both would create
        // the wrong kind of order.
        assertFalse(body.contains("product_id"))
        assertEquals(111_222L, session.trackId)
    }

    @Test
    fun `rejects a response without a gateway url`() = runTest {
        server.enqueue(
            jsonResponse(200, """{ "orderId": "ord_1", "trackId": 1 }"""),
        )

        val failure = repository.createOrder(71, shipping).exceptionOrNull()

        assertTrue(failure is Failure.InvalidResponse)
    }

    @Test
    fun `verify posts the track id, success flag and callback token`() = runTest {
        server.enqueue(jsonResponse(200, """{ "success": true, "message": "پرداخت تایید شد." }"""))

        val message = repository.verifyPayment(987_654, true, "cb-token-abc").getOrThrow()

        val request = server.takeRequest()
        assertEquals("/payment/zibal/verify", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"track_id\":\"987654\""))
        assertTrue(body.contains("\"success\":\"1\""))
        assertTrue(body.contains("\"cb_token\":\"cb-token-abc\""))
        assertEquals("پرداخت تایید شد.", message)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun jsonResponse(code: Int, body: String): MockResponse = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
