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
    /**
     * Kept for the wire, never shown.
     *
     * The bridge still ranks memberships by tier for course access, but the
     * owner sells duration — monthly, quarterly, half-year, yearly — so a tier
     * name on a plan card would be a word he never chose.
     */
    val tier: MembershipTier,
    val title: String,
    val description: String,
    val priceToman: Long,
    val originalPriceToman: Long?,
    /** Display string from the server, e.g. «۱۲ ماه». */
    val durationLabel: String,
    /** Days the plan lasts. 0 when the server sent nothing usable. */
    val durationDays: Int,
    val features: List<String>,
    val isPopular: Boolean,
    /** `amber`, `slate`, … — used to pick an accent, never relied on for meaning. */
    val colorScheme: String?,
) {
    val hasDiscount: Boolean get() = originalPriceToman != null && originalPriceToman > priceToman

    /**
     * Price per 30-day month, so the four durations can be compared at all.
     *
     * This is the number the whole ladder is about: the longer plan is not
     * "on sale", it is simply cheaper per month, and that only becomes visible
     * once both prices are divided by their own length. Null rather than zero
     * when the duration is unknown — a card must not claim ۰ تومان.
     */
    val perMonthToman: Long? get() = if (durationDays > 0) priceToman * 30 / durationDays else null

    /**
     * Percent cheaper per month than [baseline] — the shortest plan on offer.
     *
     * Null for the baseline itself and for anything that is not actually
     * cheaper: a badge that says "0% off" is worse than no badge, and a plan
     * priced above the monthly rate must not claim a saving it does not have.
     */
    fun savingsAgainst(baseline: MembershipPlan?): Int? {
        val mine = perMonthToman ?: return null
        val theirs = baseline?.perMonthToman ?: return null
        if (theirs <= 0 || durationDays <= baseline.durationDays) return null
        val saved = theirs - mine
        return if (saved > 0) ((saved * 100) / theirs).toInt() else null
    }
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
