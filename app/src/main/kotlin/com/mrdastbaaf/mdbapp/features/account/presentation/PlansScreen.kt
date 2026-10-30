package com.mrdastbaaf.mdbapp.features.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.component.AsyncStateView
import com.mrdastbaaf.mdbapp.core.ui.component.BrandTopAppBar
import com.mrdastbaaf.mdbapp.core.ui.component.PlanCard
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing

/**
 * The plans, on their own route.
 *
 * The home hero and the course screen both used to open the website for this.
 * A screen of its own means the answer to "what can I buy?" stays inside the
 * app from wherever the question is asked.
 */
@Composable
fun PlansScreen(
    onBack: () -> Unit,
    onBuyPlan: (String) -> Unit,
    viewModel: PlansViewModel = hiltViewModel(),
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = stringResource(R.string.dashboard_plans_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        AsyncStateView(
            state = plans,
            onRetry = viewModel::refresh,
            emptyMessage = stringResource(R.string.dashboard_plans_empty),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) { list ->
            // Shortest first. The server sorts by price, which is not the same
            // thing once durations differ, and a ladder that jumps 12 → 1 → 6 is
            // unreadable.
            val ladder = list.sortedBy { it.durationDays }
            val baseline = ladder.firstOrNull { it.durationDays > 0 }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                itemsIndexed(items = ladder) { _, plan ->
                    PlanCard(
                        plan = plan,
                        onBuy = { onBuyPlan(plan.id) },
                        modifier = Modifier.fillMaxWidth(),
                        savingsPercent = plan.savingsAgainst(baseline),
                    )
                }
            }
        }
    }
}
