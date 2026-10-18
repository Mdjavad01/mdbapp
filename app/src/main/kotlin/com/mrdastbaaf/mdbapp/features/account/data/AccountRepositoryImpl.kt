package com.mrdastbaaf.mdbapp.features.account.data

import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.core.network.safeApiCall
import com.mrdastbaaf.mdbapp.core.util.HtmlText
import com.mrdastbaaf.mdbapp.features.account.data.remote.dto.MembershipPlanDto
import com.mrdastbaaf.mdbapp.features.account.data.remote.dto.OrderDto
import com.mrdastbaaf.mdbapp.features.account.domain.model.MembershipPlan
import com.mrdastbaaf.mdbapp.features.account.domain.model.Order
import com.mrdastbaaf.mdbapp.features.account.domain.model.OrderStatus
import com.mrdastbaaf.mdbapp.features.account.domain.repository.AccountRepository
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The dashboard's read side.
 *
 * Two mapping rules:
 *
 *  - a plan or order without an id is dropped rather than given a synthetic one,
 *    because the id is the list key and the identity of the row;
 *  - `itemTitle` is HTML from the WooCommerce order line, so it goes through
 *    [HtmlText] like every other server-supplied label.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: MdbApi,
    private val json: Json,
) : AccountRepository {

    override suspend fun plans(): Result<List<MembershipPlan>> =
        safeApiCall(json) { api.getMembershipPlans() }
            .mapCatching { dtos -> dtos.mapNotNull { it.toDomain() } }

    override suspend fun orders(): Result<List<Order>> =
        safeApiCall(json) { api.getMyOrders() }
            .mapCatching { dto -> dto.orders.mapNotNull { it.toDomain() } }

    private fun MembershipPlanDto.toDomain(): MembershipPlan? {
        val planId = id?.takeIf { it.isNotBlank() } ?: return null
        val planTitle = HtmlText.strip(title).takeIf { it.isNotEmpty() } ?: return null

        return MembershipPlan(
            id = planId,
            tier = MembershipTier.from(tier),
            title = planTitle,
            description = HtmlText.strip(description),
            priceToman = priceToman?.coerceAtLeast(0) ?: 0,
            originalPriceToman = originalPriceToman?.takeIf { it > 0 },
            durationLabel = durationLabel?.takeIf { it.isNotBlank() }
                ?: duration?.takeIf { it.isNotBlank() }.orEmpty(),
            features = features.map { HtmlText.strip(it) }.filter { it.isNotEmpty() },
            isPopular = isPopular == true,
            colorScheme = colorScheme?.takeIf { it.isNotBlank() },
        )
    }

    private fun OrderDto.toDomain(): Order? {
        val orderId = id?.takeIf { it.isNotBlank() } ?: return null

        return Order(
            id = orderId,
            number = orderNumber.orEmpty(),
            title = HtmlText.strip(itemTitle),
            amountToman = amountToman?.coerceAtLeast(0) ?: 0,
            status = OrderStatus.from(status),
            gateway = gateway.orEmpty(),
            createdAt = createdAt.orEmpty(),
            refNumber = refNumber?.takeIf { it.isNotBlank() },
            adminNote = adminNote?.takeIf { it.isNotBlank() }?.let(HtmlText::strip),
        )
    }
}
