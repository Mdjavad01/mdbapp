package com.mrdastbaaf.mdbapp.features.shop.domain.model

/** A physical product. `id` is the WooCommerce product id. */
data class ShopProduct(
    val id: Int,
    val title: String,
    val priceToman: Long,
    val imageUrl: String?,
)

data class ShopCategory(
    val id: Int,
    val name: String,
    val products: List<ShopProduct>,
)

data class ShopCatalog(
    val featured: List<ShopProduct>,
    val categories: List<ShopCategory>,
) {
    val isEmpty: Boolean get() = featured.isEmpty() && categories.all { it.products.isEmpty() }
}

/** What the checkout form collects. The server stores it with the order. */
data class ShippingDetails(
    val fullName: String,
    val phone: String,
    val address: String,
    val postalCode: String,
)

/** An order that exists server-side and is waiting for payment. */
data class CheckoutSession(
    val orderId: String,
    val orderNumber: String,
    val trackId: Long,
    val paymentUrl: String,
)
