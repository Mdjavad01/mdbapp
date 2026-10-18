package com.mrdastbaaf.mdbapp.features.account.domain.repository

import com.mrdastbaaf.mdbapp.features.account.domain.model.MembershipPlan
import com.mrdastbaaf.mdbapp.features.account.domain.model.Order

/**
 * The read side of the dashboard. Purchases happen on the website, so there is
 * deliberately no "buy" call here — every CTA hands off to the browser.
 */
interface AccountRepository {

    /** `GET /memberships/plans` — public, cached 5 minutes server-side. */
    suspend fun plans(): Result<List<MembershipPlan>>

    /** `GET /me/orders` — Bearer required. */
    suspend fun orders(): Result<List<Order>>
}
