package com.mrdastbaaf.mdbapp.features.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.settings.ThemePreference
import com.mrdastbaaf.mdbapp.core.ui.component.BrandTopAppBar
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.theme.BrandGradient
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.account.domain.model.MembershipPlan
import com.mrdastbaaf.mdbapp.features.account.domain.model.Order
import com.mrdastbaaf.mdbapp.features.account.domain.model.OrderStatus
import com.mrdastbaaf.mdbapp.features.auth.domain.model.Membership
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipStatus
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserProfile
import com.mrdastbaaf.mdbapp.features.home.domain.model.ServerInfo

/**
 * The account tab, shaped like the web app's dashboard: who you are, what your
 * subscription is, what you can buy, and what you have bought.
 *
 * Written as a pure function of its state so every section can be previewed
 * without Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: UserProfile?,
    plans: UiState<List<MembershipPlan>>,
    orders: UiState<List<Order>>,
    serverInfo: UiState<ServerInfo>,
    themePreference: ThemePreference,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    onRetryPlans: () -> Unit,
    onRetryOrders: () -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onSignOut: () -> Unit,
    onOpenMembership: () -> Unit,
) {
    var showTechnical by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = stringResource(R.string.account_title),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_retry),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ProfileHeader(profile)

            MembershipCard(
                membership = profile?.membership,
                onOpenMembership = onOpenMembership,
            )

            SectionTitle(stringResource(R.string.dashboard_plans_title))

            SectionContent(
                state = plans,
                emptyText = stringResource(R.string.dashboard_plans_empty),
                onRetry = onRetryPlans,
            ) { list ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(items = list, key = { it.id }) { plan ->
                        PlanCard(plan = plan, onBuy = onOpenMembership)
                    }
                }
            }

            SectionTitle(stringResource(R.string.dashboard_orders_title))

            SectionContent(
                state = orders,
                emptyText = stringResource(R.string.dashboard_orders_empty),
                onRetry = onRetryOrders,
            ) { list ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        list.forEachIndexed { index, order ->
                            OrderRow(order)
                            if (index != list.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = Spacing.md),
                                )
                            }
                        }
                    }
                }
            }

            AppearanceCard(
                preference = themePreference,
                onSelected = onThemeSelected,
            )

            TextButton(
                onClick = { showTechnical = !showTechnical },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (showTechnical) {
                        stringResource(R.string.account_technical_hide)
                    } else {
                        stringResource(R.string.account_technical_show)
                    },
                )
            }

            if (showTechnical) {
                TechnicalCard(serverInfo = serverInfo, isOffline = isOffline, onRetry = onRefresh)
            }

            TextButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.dashboard_sign_out),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Profile ─────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(profile: UserProfile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile?.fullName?.trim()?.take(1).orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile?.fullName ?: stringResource(R.string.account_signed_out),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (profile != null) {
                Text(
                    text = PersianFormat.toPersianDigits(profile.maskedPhone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Membership ──────────────────────────────────────────────────────────────

@Composable
private fun MembershipCard(membership: Membership?, onOpenMembership: () -> Unit) {
    val active = membership != null && membership.status == MembershipStatus.ACTIVE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                // The gradient is painted by the Column below, so the card
                // itself must not paint over it.
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (active) Modifier.background(BrandGradient) else Modifier)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (active && membership != null) {
                Text(
                    text = stringResource(
                        R.string.dashboard_membership_active,
                        membership.tier.label(),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.dashboard_membership_days,
                        PersianFormat.formatNumber(membership.daysRemaining.toLong()),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                )
                if (membership.formattedExpireDateJalali.isNotBlank()) {
                    Text(
                        text = stringResource(
                            R.string.account_membership_expires_on,
                            PersianFormat.toPersianDigits(membership.formattedExpireDateJalali),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.dashboard_membership_none),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.dashboard_membership_none_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onOpenMembership,
                    modifier = Modifier.padding(top = Spacing.sm),
                ) {
                    Text(stringResource(R.string.access_renew))
                }
            }
        }
    }
}

@Composable
private fun MembershipTier.label(): String = when (this) {
    MembershipTier.BRONZE -> stringResource(R.string.tier_bronze)
    MembershipTier.SILVER -> stringResource(R.string.tier_silver)
    MembershipTier.GOLD -> stringResource(R.string.tier_gold)
    MembershipTier.FREE -> stringResource(R.string.dashboard_membership_none)
}

// ── Plans ───────────────────────────────────────────────────────────────────

@Composable
private fun PlanCard(plan: MembershipPlan, onBuy: () -> Unit) {
    Card(
        modifier = Modifier.width(250.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isPopular) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (plan.isPopular) {
                Text(
                    text = stringResource(R.string.dashboard_plan_popular),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (plan.durationLabel.isNotBlank()) {
                Text(
                    text = PersianFormat.toPersianDigits(plan.durationLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = PersianFormat.formatToman(plan.priceToman),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                plan.originalPriceToman?.let { original ->
                    Text(
                        text = PersianFormat.formatToman(original),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
            }

            plan.features.take(3).forEach { feature ->
                Text(
                    text = "• $feature",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            Button(
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.dashboard_plan_buy))
            }
        }
    }
}

// ── Orders ──────────────────────────────────────────────────────────────────

@Composable
private fun OrderRow(order: Order) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = order.title.ifBlank { order.number },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(Spacing.sm))
            StatusChip(order.status)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = PersianFormat.formatToman(order.amountToman),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (order.createdAt.isNotBlank()) {
                Text(
                    text = PersianFormat.toPersianDigits(order.createdAt.substringBefore(' ')),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        order.adminNote?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val color = when {
        status.isPaid -> MaterialTheme.colorScheme.primary
        status.isOpen -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Text(
        text = status.label(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun OrderStatus.label(): String = when (this) {
    OrderStatus.PENDING -> stringResource(R.string.order_status_pending)
    OrderStatus.PROCESSING -> stringResource(R.string.order_status_processing)
    OrderStatus.VERIFYING -> stringResource(R.string.order_status_verifying)
    OrderStatus.COMPLETED -> stringResource(R.string.order_status_completed)
    OrderStatus.FAILED -> stringResource(R.string.order_status_failed)
    OrderStatus.REJECTED -> stringResource(R.string.order_status_rejected)
    OrderStatus.CANCELLED -> stringResource(R.string.order_status_cancelled)
    OrderStatus.UNKNOWN -> stringResource(R.string.order_status_unknown)
}

// ── Appearance ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceCard(
    preference: ThemePreference,
    onSelected: (ThemePreference) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.dashboard_appearance_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ThemeChip(
                    label = stringResource(R.string.theme_system),
                    selected = preference == ThemePreference.SYSTEM,
                    onClick = { onSelected(ThemePreference.SYSTEM) },
                )
                ThemeChip(
                    label = stringResource(R.string.theme_light),
                    selected = preference == ThemePreference.LIGHT,
                    onClick = { onSelected(ThemePreference.LIGHT) },
                )
                ThemeChip(
                    label = stringResource(R.string.theme_dark),
                    selected = preference == ThemePreference.DARK,
                    onClick = { onSelected(ThemePreference.DARK) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

// ── Technical ───────────────────────────────────────────────────────────────

@Composable
private fun TechnicalCard(
    serverInfo: UiState<ServerInfo>,
    isOffline: Boolean,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            when (serverInfo) {
                UiState.Loading -> Text(
                    text = stringResource(R.string.state_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is UiState.Error -> {
                    Text(
                        text = if (isOffline) {
                            stringResource(R.string.state_offline_message)
                        } else {
                            serverInfo.failure.userMessage
                                ?: stringResource(R.string.state_error_message)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.action_retry))
                    }
                }

                is UiState.Empty -> Unit

                is UiState.Success -> {
                    InfoRow("نسخهٔ افزونه", serverInfo.data.pluginVersion)
                    InfoRow("نسخهٔ وب‌اپ", serverInfo.data.pwaVersion)
                    InfoRow("نسخهٔ دیتابیس", serverInfo.data.databaseVersion)
                    InfoRow("زمان سرور", serverInfo.data.serverTime)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Shared ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}

/**
 * Renders one section's three states inline.
 *
 * `AsyncStateView` fills its parent, which collapses inside a scrolling Column —
 * this screen needs sections that size themselves, so the states are spelled out
 * rather than reused.
 */
@Composable
private fun <T> SectionContent(
    state: UiState<T>,
    emptyText: String,
    onRetry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.state_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is UiState.Empty -> Text(
            text = emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is UiState.Error -> Column {
            Text(
                text = state.failure.userMessage
                    ?: stringResource(R.string.state_error_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.action_retry))
            }
        }

        is UiState.Success -> content(state.data)
    }
}
