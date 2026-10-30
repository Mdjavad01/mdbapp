package com.mrdastbaaf.mdbapp.features.account.domain

import com.mrdastbaaf.mdbapp.features.account.domain.model.MembershipPlan
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plan ladder is arithmetic, so it is tested as arithmetic.
 *
 * The owner types one total price per plan. Everything the card claims —
 * "per month", "12% cheaper" — is derived from that number and the duration, so
 * a mistake here would print a false saving on a real payment screen.
 */
class MembershipPlanTest {

    private fun plan(
        id: String,
        days: Int,
        price: Long,
        original: Long? = null,
        label: String = "$days روزه",
    ) = MembershipPlan(
        id = id,
        tier = MembershipTier.BRONZE,
        title = label,
        description = "",
        priceToman = price,
        originalPriceToman = original,
        durationLabel = label,
        durationDays = days,
        features = emptyList(),
        isPopular = false,
        colorScheme = null,
    )

    private val monthly = plan("monthly", 30, 490_000)
    private val quarterly = plan("quarterly", 90, 1_290_000)
    private val halfYear = plan("half", 180, 2_390_000)
    private val yearly = plan("yearly", 365, 4_290_000)

    @Test
    fun `per month divides the total by the duration`() {
        assertEquals(490_000L, monthly.perMonthToman)
        assertEquals(430_000L, quarterly.perMonthToman)
        assertEquals(398_333L, halfYear.perMonthToman)
        assertEquals(352_602L, yearly.perMonthToman)
    }

    @Test
    fun `a plan with no duration has no per-month price`() {
        assertNull(plan("broken", 0, 500_000).perMonthToman)
        assertNull(plan("broken", 0, 500_000).savingsAgainst(monthly))
    }

    @Test
    fun `longer plans report the saving against the monthly one`() {
        assertEquals(12, quarterly.savingsAgainst(monthly))
        assertEquals(18, halfYear.savingsAgainst(monthly))
        assertEquals(28, yearly.savingsAgainst(monthly))
    }

    @Test
    fun `the baseline itself claims no saving`() {
        assertNull(monthly.savingsAgainst(monthly))
        assertNull(monthly.savingsAgainst(null))
    }

    @Test
    fun `a longer plan that is not actually cheaper claims nothing`() {
        // 90 days for 1.6m is 533k a month — dearer than paying monthly. The
        // card must stay silent rather than advertise a negative discount.
        val overpriced = plan("overpriced", 90, 1_600_000)
        assertNull(overpriced.savingsAgainst(monthly))
    }

    @Test
    fun `a longer plan is never compared to a longer baseline`() {
        assertNull(monthly.savingsAgainst(yearly))
    }

    @Test
    fun `a struck-through price is only a discount when it is higher`() {
        assertTrue(plan("a", 30, 100, original = 150).hasDiscount)
        assertTrue(!plan("b", 30, 100, original = 100).hasDiscount)
        assertTrue(!plan("c", 30, 100).hasDiscount)
    }
}
