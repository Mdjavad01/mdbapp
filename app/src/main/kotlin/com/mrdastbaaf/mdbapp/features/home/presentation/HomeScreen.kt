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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.mrdastbaaf.mdbapp.features.home.domain.model.ServerInfo

/**
 * Phase P0 scaffold screen.
 *
 * This is not a product screen and is replaced in P4. Its purpose is to make the
 * foundation visible and verifiable:
 *
 *  - the layout is RTL (labels start on the right, key/value rows read correctly)
 *  - Vazirmatn renders Persian and Persian-Indic digits
 *  - the environment and base URL shown are the ones actually compiled in, so a
 *    mis-flavoured build is obvious at a glance
 *  - a live `GET /version` proves the whole network stack reaches the bridge
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<ServerInfo>,
    isOffline: Boolean,
    onRetry: () -> Unit,
) {
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
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ScaffoldIntro()

            AsyncStateView(
                state = state,
                isOffline = isOffline,
                onRetry = onRetry,
                modifier = Modifier.height(220.dp),
            ) { info ->
                ServerInfoCard(info)
            }

            EnvironmentCard()
        }
    }
}

@Composable
private fun ScaffoldIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.p0_badge),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.p0_headline),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.p0_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.p0_font_sample),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

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

@Preview(name = "خانه — موفق", showBackground = true, locale = "fa")
@Composable
private fun HomeScreenPreview() {
    MdbTheme {
        HomeScreen(
            state = UiState.Success(
                ServerInfo(
                    pwaVersion = "1.5.6",
                    pluginVersion = "1.7.13",
                    databaseVersion = "1.5.1",
                    serverTime = "2026-10-16 12:00:00",
                    serverEpochSeconds = 1760625600L,
                ),
            ),
            isOffline = false,
            onRetry = {},
        )
    }
}
