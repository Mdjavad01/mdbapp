package com.mrdastbaaf.mdbapp.features.account.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.settings.ThemePreference
import com.mrdastbaaf.mdbapp.core.ui.component.BrandTopAppBar
import com.mrdastbaaf.mdbapp.core.ui.component.PlanCard
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.theme.BrandGradient
import com.mrdastbaaf.mdbapp.core.ui.theme.LocalBrand
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.account.domain.model.MembershipPlan
import com.mrdastbaaf.mdbapp.features.account.domain.model.Order
import com.mrdastbaaf.mdbapp.features.account.domain.model.OrderStatus
import com.mrdastbaaf.mdbapp.features.auth.domain.model.Membership
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipStatus
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserProfile
import com.mrdastbaaf.mdbapp.features.home.domain.model.ServerInfo

/**
 * The account tab.
 *
 * Everything the user needs to know about themselves in one scroll: who they
 * are, what their subscription is, what they can buy, and what they have
 * bought. The sections arrive in a short stagger rather than all at once — one
 * entrance, no continuous animation, so scrolling stays at full frame rate.
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
    onBuyPlan: (String) -> Unit,
    onSignOut: () -> Unit,
    onOpenPlans: () -> Unit,
) {
    var showTechnical by rememberSaveable { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

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
            ProfileHero(
                profile = profile,
                onOpenPlans = onOpenPlans,
            )

            Reveal(entered, index = 0) {
                SectionTitle(stringResource(R.string.dashboard_plans_title))

                SectionContent(
                    state = plans,
                    emptyText = stringResource(R.string.dashboard_plans_empty),
                    onRetry = onRetryPlans,
                ) { list ->
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        itemsIndexed(items = list) { _, plan ->
                            PlanCard(
                                plan = plan,
                                onBuy = { onBuyPlan(plan.id) },
                                modifier = Modifier.width(260.dp),
                            )
                        }
                    }
                }
            }

            Reveal(entered, index = 1) {
                SectionTitle(stringResource(R.string.dashboard_orders_title))

                SectionContent(
                    state = orders,
                    emptyText = stringResource(R.string.dashboard_orders_empty),
                    onRetry = onRetryOrders,
                ) { list ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        list.forEach { order -> OrderCard(order) }
                    }
                }
            }

            Reveal(entered, index = 2) {
                AppearanceCard(preference = themePreference, onSelected = onThemeSelected)
            }

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
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (showTechnical) {
                TechnicalCard(serverInfo = serverInfo, isOffline = isOffline, onRetry = onRefresh)
            }

            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dashboard_sign_out),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

/**
 * A one-shot entrance per section.
 *
 * The delay is index × 60 ms: enough to read as a sequence, short enough that
 * the last section is on screen before the eye reaches it. Nothing animates
 * afterwards, so a long scroll costs no extra frames.
 */
@Composable
private fun Reveal(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 280, delayMillis = index * 60)) +
            slideInVertically(tween(durationMillis = 340, delayMillis = index * 60)) { it / 12 },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            content()
        }
    }
}

// ── Profile ─────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHero(profile: UserProfile?, onOpenPlans: () -> Unit) {
    val membership = profile?.membership
    val active = membership != null && membership.status == MembershipStatus.ACTIVE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(BrandGradient)
            .padding(Spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile?.fullName?.trim()?.take(1).orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.fullName ?: stringResource(R.string.account_signed_out),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile != null) {
                        Text(
                            text = PersianFormat.toPersianDigits(profile.maskedPhone),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                    }
                }
            }

            if (active && membership != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    // No tier name: the owner sells duration, and "برنزی" is a
                    // word he never chose. What matters to the member is that it
                    // is active and how long is left.
                    HeroPill(
                        text = stringResource(R.string.dashboard_membership_active_plain),
                    )
                    HeroPill(
                        text = stringResource(
                            R.string.dashboard_membership_days,
                            PersianFormat.formatNumber(membership.daysRemaining.toLong()),
                        ),
                    )
                }

                if (membership.formattedExpireDateJalali.isNotBlank()) {
                    Text(
                        text = stringResource(
                            R.string.account_membership_expires_on,
                            PersianFormat.toPersianDigits(membership.formattedExpireDateJalali),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_membership_none_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onOpenPlans,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = LocalBrand.current.primary,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.access_renew),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
    )
}

// ── Plans ───────────────────────────────────────────────────────────────────

// ── Orders ──────────────────────────────────────────────────────────────────

@Composable
private fun OrderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = PersianFormat.formatToman(order.amountToman),
                    style = MaterialTheme.typography.labelLarge,
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
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val color = when {
        status.isPaid -> MaterialTheme.colorScheme.primary
        status.isOpen -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = Spacing.sm, vertical = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = status.label(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
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
        shape = RoundedCornerShape(18.dp),
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
        shape = RoundedCornerShape(18.dp),
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.padding(top = Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
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
