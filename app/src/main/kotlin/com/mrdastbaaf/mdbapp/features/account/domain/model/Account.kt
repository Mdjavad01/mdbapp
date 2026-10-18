package com.mrdastbaaf.mdbapp.features.account.domain.model

import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier

/**
 * A purchasable subscription.
 *
 * `priceToman` is what the user pays; [originalPriceToman] exists only to draw
 * a struck-through comparison, and is `null` when there is no discount.
 */
data class MembershipPlan(
    val id: String,
    val tier: MembershipTier,
    val title: String,
    val description: String,
    val priceToman: Long,
    val originalPriceToman: Long?,
    /** Display string from the server, e.g. «۱۲ ماه». */
    val durationLabel: String,
    val features: List<String>,
    val isPopular: Boolean,
    /** `amber`, `slate`, … — used to pick an accent, never relied on for meaning. */
    val colorScheme: String?,
) {
    val hasDiscount: Boolean get() = originalPriceToman != null && originalPriceToman > priceToman
}

/** One purchase, as the dashboard lists it. */
data class Order(
    val id: String,
    val number: String,
    val title: String,
    val amountToman: Long,
    val status: OrderStatus,
    /** `zibal` or `card_to_card`. */
    val gateway: String,
    /** MySQL local time, shown as-is: the server owns the timezone. */
    val createdAt: String,
    val refNumber: String?,
    /** Only when a card-to-card transfer was rejected. */
    val adminNote: String?,
)

enum class OrderStatus {
    PENDING,
    PROCESSING,
    VERIFYING,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED,
    UNKNOWN,
    ;

    /** True when the money actually arrived. */
    val isPaid: Boolean get() = this == COMPLETED

    /** True while the outcome is still open. */
    val isOpen: Boolean get() = this == PENDING || this == PROCESSING || this == VERIFYING

    companion object {
        fun from(raw: String?): OrderStatus = when (raw?.lowercase()) {
            "pending" -> PENDING
            "processing" -> PROCESSING
            "verifying" -> VERIFYING
            "completed" -> COMPLETED
            "failed" -> FAILED
            "rejected" -> REJECTED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}
