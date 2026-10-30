package com.mrdastbaaf.mdbapp.features.shop.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /shop/products` — the physical catalogue. */
@Serializable
data class ShopProductsDto(
    val success: Boolean? = null,
    val featured: List<ShopProductDto> = emptyList(),
    val categories: List<ShopCategoryDto> = emptyList(),
)

/** `GET /shop/categories` — the tree only, no products. */
@Serializable
data class ShopCategoriesDto(
    val success: Boolean? = null,
    val categories: List<ShopCategoryDto> = emptyList(),
)

/** `GET /shop/catalog` — one page of one category. */
@Serializable
data class ShopCatalogPageDto(
    val success: Boolean? = null,
    val category: ShopCategoryDto? = null,
    val page: Int? = null,
    val perPage: Int? = null,
    val total: Int? = null,
    val totalPages: Int? = null,
    val products: List<ShopProductDto> = emptyList(),
)

@Serializable
data class ShopProductDto(
    val id: Int? = null,
    val title: String? = null,
    /** Integer Toman. */
    val price: Long? = null,
    val imageUrl: String? = null,
    /** WooCommerce permalink — the app does not open it, but it identifies the product. */
    val url: String? = null,
)

@Serializable
data class ShopCategoryDto(
    val id: Int? = null,
    val slug: String? = null,
    val name: String? = null,
    /** Term id of the parent category, 0 for a root. Added in bridge 1.9.0. */
    val parent: Int? = null,
    /** The category's own product count. Absent on older bridges. */
    val count: Int? = null,
    val products: List<ShopProductDto> = emptyList(),
)

/**
 * Order creation.
 *
 * Field names are snake_case because the bridge reads them with
 * `$request->get_param('product_id')`, not from a nested object.
 */
@Serializable
data class ShopOrderRequestDto(
    @SerialName("product_id") val productId: Int,
    /**
     * Deliberately **not** defaulted: kotlinx.serialization omits defaulted
     * values, so `kind = "shop"` as a default would never reach the server and
     * the bridge would fall back to the course-purchase branch.
     */
    val kind: String,
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val address: String,
    @SerialName("postal_code") val postalCode: String,
)

/**
 * Subscription purchase. The bridge branches on which id is present, so a plan
 * order must send `plan_id` and **no** `product_id`.
 */
@Serializable
data class PlanOrderRequestDto(
    @SerialName("plan_id") val planId: String,
)

@Serializable
data class ShopOrderResponseDto(
    val orderId: String? = null,
    val orderNumber: String? = null,
    val trackId: Long? = null,
    /** `https://gateway.zibal.ir/start/{trackId}` */
    val paymentUrl: String? = null,
)

@Serializable
data class VerifyPaymentRequestDto(
    @SerialName("track_id") val trackId: String,
    val success: String,
    @SerialName("cb_token") val cbToken: String,
)

@Serializable
data class VerifyPaymentResponseDto(
    val success: Boolean? = null,
    val message: String? = null,
)
