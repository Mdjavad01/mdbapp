package com.mrdastbaaf.mdbapp.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.config.AppConfig
import com.mrdastbaaf.mdbapp.core.config.Environment
import com.mrdastbaaf.mdbapp.core.ui.component.AsyncStateView
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.auth.domain.model.Membership
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipStatus
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserProfile
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserRole
import com.mrdastbaaf.mdbapp.features.auth.presentation.session.SessionUiState
import com.mrdastbaaf.mdbapp.features.home.domain.model.ServerInfo

/**
 * The account tab.
 *
 * Two jobs:
 *
 *  1. The account panel is the real one — OTP, token storage, session, profile
 *     fetch and logout, all against the live backend.
 *  2. The server and build cards stay because they are the fastest way to
 *     answer "which plugin is this build talking to?" during a rollout.
 *
 * It is written as a pure function of its state so both halves can be previewed
 * and unit-tested without Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<ServerInfo>,
    sessionState: SessionUiState,
    isOffline: Boolean,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRefreshProfile: () -> Unit,
) {
    var showTechnical by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
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
            AccountCard(
                sessionState = sessionState,
                onSignIn = onSignIn,
                onSignOut = onSignOut,
                onRefresh = onRefreshProfile,
            )

            // The version and build cards answer "which server is this build
            // talking to?" during a rollout, but they are not something a
            // student should have to scroll past on their account screen.
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
                AsyncStateView(
                    state = state,
                    isOffline = isOffline,
                    onRetry = onRetry,
                    modifier = Modifier.height(240.dp),
                ) { info ->
                    ServerInfoCard(info)
                }

                EnvironmentCard()
            }
        }
    }
}

// ── Account ─────────────────────────────────────────────────────────────────

@Composable
private fun AccountCard(
    sessionState: SessionUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRefresh: () -> Unit,
) {
    InfoCard(title = stringResource(R.string.account_title)) {
        val profile = sessionState.profile

        when {
            !sessionState.isSignedIn -> {
                Text(
                    text = stringResource(R.string.account_signed_out),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.account_signed_out_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                OutlinedButton(onClick = onSignIn) {
                    Text(stringResource(R.string.account_sign_in))
                }
            }

            profile == null -> {
                // Signed in but the profile has not arrived yet (first fetch in
                // flight, or it failed). The session is still valid — the token
                // is what matters — so this is not an error state.
                Text(
                    text = stringResource(R.string.state_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                InfoRow("شماره موبایل", PersianFormat.toPersianDigits(profile.maskedPhone))
                InfoRow("شناسهٔ کاربر", PersianFormat.toPersianDigits(profile.id))
                MembershipRow(profile)

                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TextButton(onClick = onRefresh) {
                        Text(stringResource(R.string.account_refresh))
                    }
                    TextButton(onClick = onSignOut) {
                        Text(stringResource(R.string.auth_logout))
                    }
                }
            }
        }

        sessionState.errorMessage?.let { message ->
            Text(
                text = message.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun MembershipRow(profile: UserProfile) {
    val membership = profile.membership

    if (membership == null || membership.status != MembershipStatus.ACTIVE) {
        Text(
            text = stringResource(R.string.account_no_membership),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Text(
        text = stringResource(
            R.string.account_membership_active,
            membership.tier.displayName(),
            PersianFormat.formatNumber(membership.daysRemaining.toLong()),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
    )

    // The server pre-formats the Jalali date, so it is shown verbatim rather
    // than re-derived — the bridge owns the timezone handling.
    if (membership.formattedExpireDateJalali.isNotBlank()) {
        Text(
            text = stringResource(
                R.string.account_membership_expires_on,
                PersianFormat.toPersianDigits(membership.formattedExpireDateJalali),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MembershipTier.displayName(): String = when (this) {
    MembershipTier.BRONZE -> stringResource(R.string.tier_bronze)
    MembershipTier.SILVER -> stringResource(R.string.tier_silver)
    MembershipTier.GOLD -> stringResource(R.string.tier_gold)
    MembershipTier.FREE -> stringResource(R.string.account_no_membership)
}

// ── Diagnostics ─────────────────────────────────────────────────────────────

@Composable
private fun ServerInfoCard(info: ServerInfo) {
    InfoCard(title = "وضعیت سرور") {
        InfoRow("نسخهٔ افزونه", info.pluginVersion)
        InfoRow("نسخهٔ وب‌اپ", info.pwaVersion)
        InfoRow("نسخهٔ دیتابیس", info.databaseVersion)
        InfoRow("زمان سرور", info.serverTime)
        info.serverEpochSeconds?.let { epoch ->
            InfoRow("مهر زمانی", PersianFormat.formatNumber(epoch))
        }
    }
}

@Composable
private fun EnvironmentCard() {
    InfoCard(title = "پیکربندی بیلد") {
        InfoRow(stringResource(R.string.p0_env_label), AppConfig.environment.displayName())
        InfoRow(stringResource(R.string.p0_api_label), AppConfig.baseUrl, wrap = true)
        InfoRow(stringResource(R.string.p0_version_label), AppConfig.versionName)
        InfoRow("قفل صفحه", if (AppConfig.strictSecurityEnabled) "فعال" else "غیرفعال")
        InfoRow("پین گواهی", if (AppConfig.certPinningEnabled) "فعال" else "غیرفعال")
    }
}

// ── Building blocks ─────────────────────────────────────────────────────────

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.xs))
            content()
        }
    }
}

/**
 * A label/value row.
 *
 * `Row` in an RTL layout puts the first child on the right, so the label
 * naturally reads first — no manual ordering needed.
 */
@Composable
private fun InfoRow(label: String, value: String, wrap: Boolean = false) {
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
            modifier = if (wrap) Modifier.weight(1f) else Modifier,
        )
    }
}

private fun Environment.displayName(): String = when (this) {
    Environment.DEVELOPMENT -> "توسعه"
    Environment.STAGING -> "استیجینگ"
    Environment.PRODUCTION -> "پروداکشن"
}

// ── Previews ────────────────────────────────────────────────────────────────

private val PreviewServerInfo = ServerInfo(
    pwaVersion = "1.5.6",
    pluginVersion = "1.7.13",
    databaseVersion = "1.5.1",
    serverTime = "2026-10-16 12:00:00",
    serverEpochSeconds = 1760625600L,
)

@Preview(name = "خانه — وارد نشده", showBackground = true, locale = "fa")
@Composable
private fun HomeSignedOutPreview() {
    MdbTheme {
        HomeScreen(
            state = UiState.Success(PreviewServerInfo),
            sessionState = SessionUiState(isSignedIn = false),
            isOffline = false,
            onRetry = {},
            onSignIn = {},
            onSignOut = {},
            onRefreshProfile = {},
        )
    }
}

@Preview(name = "خانه — وارد شده", showBackground = true, locale = "fa")
@Composable
private fun HomeSignedInPreview() {
    MdbTheme {
        HomeScreen(
            state = UiState.Success(PreviewServerInfo),
            sessionState = SessionUiState(
                isSignedIn = true,
                profile = UserProfile(
                    id = "42",
                    phoneNumber = "09123456789",
                    maskedPhone = "0912***6789",
                    fullName = "محمدجواد فلان",
                    role = UserRole.STUDENT,
                    membership = Membership(
                        tier = MembershipTier.GOLD,
                        planId = "gold_4821",
                        planName = "اشتراک طلایی",
                        status = MembershipStatus.ACTIVE,
                        startedAt = "2026-01-01 10:00:00",
                        expiresAt = "2027-01-01 10:00:00",
                        formattedExpireDateJalali = "1405/10/11",
                        daysRemaining = 77,
                    ),
                ),
            ),
            isOffline = false,
            onRetry = {},
            onSignIn = {},
            onSignOut = {},
            onRefreshProfile = {},
        )
    }
}
