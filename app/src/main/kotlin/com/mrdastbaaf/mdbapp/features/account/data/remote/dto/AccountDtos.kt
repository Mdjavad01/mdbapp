package com.mrdastbaaf.mdbapp.features.account.data.remote.dto

import kotlinx.serialization.Serializable

/** `GET /memberships/plans` — a **bare array**. */
@Serializable
data class MembershipPlanDto(
    val id: String? = null,
    val tier: String? = null,
    val title: String? = null,
    val description: String? = null,
    val priceToman: Long? = null,
    /** Present only while a discount is running. */
    val originalPriceToman: Long? = null,
    /** `"{days}_days"` — machine value. Use [durationLabel] for display. */
    val duration: String? = null,
    val durationLabel: String? = null,
    /** Added in bridge 1.9.0. Absent on older builds — [duration] is the fallback. */
    val durationDays: Int? = null,
    val features: List<String> = emptyList(),
    val isPopular: Boolean? = null,
    val isActive: Boolean? = null,
    /** `slate | amber | stone | yellow` — a hint, not a contract. */
    val colorScheme: String? = null,
)

/** `GET /me/orders` — `{ orders: [...] }`, deliberately without a `success` key. */
@Serializable
data class OrdersDto(
    val orders: List<OrderDto> = emptyList(),
)

@Serializable
data class OrderDto(
    val id: String? = null,
    val orderNumber: String? = null,
    val itemTitle: String? = null,
    val amountToman: Long? = null,
    val gateway: String? = null,
    /** `pending | processing | verifying | completed | failed | rejected | cancelled` */
    val status: String? = null,
    /** MySQL local time, not ISO 8601. */
    val createdAt: String? = null,
    val verifiedAt: String? = null,
    val refNumber: String? = null,
    /** Only for a rejected card-to-card transfer. */
    val adminNote: String? = null,
)
