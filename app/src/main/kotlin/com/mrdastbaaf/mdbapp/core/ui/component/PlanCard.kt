package com.mrdastbaaf.mdbapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mrdastbaaf.mdbapp.core.ui.theme.BrandGradient
import com.mrdastbaaf.mdbapp.core.ui.theme.LocalBrand
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.account.domain.model.MembershipPlan

/**
 * A subscription plan.
 *
 * Shared by the account tab's rail and the standalone plans screen: same card,
 * different width. The highlighted plan is painted with the brand gradient and
 * gets a white CTA, because on a gradient a filled primary button disappears.
 */
@Composable
fun PlanCard(
    plan: MembershipPlan,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
    /** How much cheaper per month than the shortest plan, when that applies. */
    savingsPercent: Int? = null,
) {
    val highlighted = plan.isPopular
    val contentColor = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (highlighted) {
        Color.White.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (highlighted) Modifier.background(BrandGradient) else Modifier)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (plan.isPopular) {
                Text(
                    text = stringResource(R.string.dashboard_plan_popular),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = Spacing.sm, vertical = 2.dp),
                )
            }

            // The duration is the axis: these plans differ only in how long they
            // last, so the length is the headline and the owner's title is the
            // qualifier. When he typed the same words in both fields — which is
            // what the panel invites — the duplicate is dropped rather than
            // printed twice.
            val headline = plan.durationLabel.ifBlank { plan.title }
            val subtitle = plan.title.takeIf { it != headline }

            Text(
                text = PersianFormat.toPersianDigits(headline),
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (subtitle != null) {
                Text(
                    text = PersianFormat.toPersianDigits(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = PersianFormat.formatToman(plan.priceToman),
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                )
                plan.originalPriceToman?.let { original ->
                    Text(
                        text = PersianFormat.formatToman(original),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
            }

            plan.perMonthToman?.let { perMonth ->
                Text(
                    text = stringResource(
                        R.string.plan_per_month,
                        PersianFormat.formatToman(perMonth),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = mutedColor,
                )
            }

            if (savingsPercent != null && savingsPercent > 0) {
                Text(
                    text = stringResource(
                        R.string.plan_savings,
                        PersianFormat.toPersianDigits(savingsPercent.toString()),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlighted) Color.White else MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (highlighted) {
                                Color.White.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                        .padding(horizontal = Spacing.sm, vertical = 2.dp),
                )
            }

            plan.features.take(3).forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (highlighted) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            Button(
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth(),
                colors = if (highlighted) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LocalBrand.current.primary,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(stringResource(R.string.dashboard_plan_buy))
            }
        }
    }
}
