package com.mrdastbaaf.mdbapp.features.shop.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.core.network.safeApiCall
import com.mrdastbaaf.mdbapp.core.util.HtmlText
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.PlanOrderRequestDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.ShopOrderRequestDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.ShopOrderResponseDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.ShopProductDto
import com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.VerifyPaymentRequestDto
import com.mrdastbaaf.mdbapp.features.shop.domain.model.CheckoutSession
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCatalog
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCategory
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopProduct
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShippingDetails
import com.mrdastbaaf.mdbapp.features.shop.domain.repository.ShopRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The shop catalogue and the checkout handshake.
 *
 * The catalogue is cached in memory because the checkout screen needs the
 * product's title and price for its summary, and refetching the whole catalogue
 * to draw one row would be wasteful on a mobile connection. [catalog] with
 * `force = true` bypasses the cache for the pull-to-refresh action.
 */
@Singleton
class ShopRepositoryImpl @Inject constructor(
    private val api: MdbApi,
    private val json: Json,
) : ShopRepository {

    private var cached: ShopCatalog? = null

    override suspend fun catalog(force: Boolean): Result<ShopCatalog> {
        if (!force) {
            cached?.let { return Result.success(it) }
        }

        return safeApiCall(json) { api.getShopProducts() }
            .mapCatching { dto ->
                ShopCatalog(
                    featured = dto.featured.mapNotNull { it.toDomain() },
                    categories = dto.categories.mapNotNull { it.toDomain() },
                )
            }
            .onSuccess { cached = it }
    }

    override suspend fun product(id: Int): ShopProduct? {
        val catalog = cached ?: catalog().getOrNull() ?: return null
        return catalog.featured.firstOrNull { it.id == id }
            ?: catalog.categories.asSequence()
                .flatMap { it.products.asSequence() }
                .firstOrNull { it.id == id }
    }

    override suspend fun createOrder(
        productId: Int,
        shipping: ShippingDetails,
    ): Result<CheckoutSession> = safeApiCall(json) {
        api.initiateShopOrder(
            ShopOrderRequestDto(
                productId = productId,
                kind = "shop",
                fullName = shipping.fullName,
                phone = shipping.phone,
                address = shipping.address,
                postalCode = shipping.postalCode,
            ),
        )
    }.mapCatching { it.toSession() }

    private fun ShopOrderResponseDto.toSession(): CheckoutSession {
        val orderId = orderId?.takeIf { it.isNotBlank() }
        val track = trackId
        val url = paymentUrl?.takeIf { it.isNotBlank() }

        if (orderId == null || track == null || url == null) {
            throw Failure.InvalidResponse("اطلاعات پرداخت از سرور ناقص بود.")
        }

        return CheckoutSession(
            orderId = orderId,
            orderNumber = orderNumber.orEmpty(),
            trackId = track,
            paymentUrl = url,
        )
    }

    override suspend fun createPlanOrder(planId: String): Result<CheckoutSession> =
        safeApiCall(json) { api.initiatePlanOrder(PlanOrderRequestDto(planId = planId)) }
            .mapCatching { it.toSession() }

    override suspend fun verifyPayment(
        trackId: Long,
        success: Boolean,
        cbToken: String,
    ): Result<String?> = safeApiCall(json) {
        api.verifyPayment(
            VerifyPaymentRequestDto(
                trackId = trackId.toString(),
                success = if (success) "1" else "0",
                cbToken = cbToken,
            ),
        )
    }.map { it.message }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private fun ShopProductDto.toDomain(): ShopProduct? {
        val productId = id?.takeIf { it > 0 } ?: return null
        val productTitle = HtmlText.strip(title).takeIf { it.isNotEmpty() } ?: return null

        return ShopProduct(
            id = productId,
            title = productTitle,
            priceToman = price?.coerceAtLeast(0) ?: 0,
            imageUrl = imageUrl?.takeIf { it.isNotBlank() },
        )
    }

    private fun com.mrdastbaaf.mdbapp.features.shop.data.remote.dto.ShopCategoryDto.toDomain(): ShopCategory? {
        val categoryId = id ?: return null
        // The bridge synthesises a "سایر" bucket for uncategorised products,
        // and it arrives with an empty name.
        val categoryName = HtmlText.strip(name).ifEmpty { if (categoryId == 0) "سایر" else "" }
        if (categoryName.isEmpty()) return null

        // An empty category is kept rather than dropped: since bridge 1.9.0 the
        // response includes ancestors that hold no ticked products of their own,
        // and dropping one would orphan every subcategory under it.
        return ShopCategory(
            id = categoryId,
            name = categoryName,
            products = products.mapNotNull { it.toDomain() },
            parentId = parent ?: 0,
        )
    }
}
