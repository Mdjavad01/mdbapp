package com.mrdastbaaf.mdbapp.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.theme.BrandGradient
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing

/**
 * The gradient block at the top of both sign-in steps.
 *
 * Shared so the two screens read as one flow: same curve, same padding, same
 * place for the back arrow. The logo is optional because step two is about the
 * code, not about introducing the brand again.
 */
@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showLogo: Boolean = true,
    onBack: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = BrandGradient,
                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
            )
            .statusBarsPadding()
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.sm,
                bottom = Spacing.xxl,
            ),
    ) {
        Column {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White,
                    )
                }
            }

            if (showLogo) {
                Image(
                    painter = painterResource(R.drawable.logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(84.dp),
                )
                Spacer(Modifier.height(Spacing.md))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )

            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }
    }
}

/**
 * The primary action of the sign-in flow.
 *
 * A `Button` cannot take a brush, so this is a clipped box with the brand
 * gradient and a ripple bounded by the same shape. Disabled state fades rather
 * than greys out, which keeps the gradient intact.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = tween(durationMillis = 220),
        label = "gradient-button-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(18.dp))
            .background(BrandGradient)
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
