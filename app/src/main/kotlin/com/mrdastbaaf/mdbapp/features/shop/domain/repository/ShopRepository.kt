package com.mrdastbaaf.mdbapp.features.shop.domain.repository

import com.mrdastbaaf.mdbapp.features.shop.domain.model.CheckoutSession
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCatalog
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopProduct
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShippingDetails

/**
 * The physical shop and its checkout.
 *
 * Payment is the one flow where the app creates server state: [createOrder]
 * produces a real order and a gateway URL, and [verifyPayment] settles it. Both
 * are called from the app so the user never leaves it — but the card data is
 * entered on the gateway's own page inside a WebView and never touches this
 * code.
 */
interface ShopRepository {

    /** Cached after the first success; [force] refetches. */
    suspend fun catalog(force: Boolean = false): Result<ShopCatalog>

    /** The product the checkout screen is about, from the cached catalogue. */
    suspend fun product(id: Int): ShopProduct?

    suspend fun createOrder(productId: Int, shipping: ShippingDetails): Result<CheckoutSession>

    /**
     * The same handshake for a subscription.
     *
     * Lives here rather than in the account repository because it is the same
     * gateway, the same order table and the same verification — only the id the
     * bridge branches on differs.
     */
    suspend fun createPlanOrder(planId: String): Result<CheckoutSession>

    /** Confirms the payment with the token from the gateway redirect. */
    suspend fun verifyPayment(trackId: Long, success: Boolean, cbToken: String): Result<String?>
}
