package com.mrdastbaaf.mdbapp.features.appcontent.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.component.AsyncStateView
import com.mrdastbaaf.mdbapp.core.ui.component.BrandTopAppBar
import com.mrdastbaaf.mdbapp.core.ui.component.KilimPattern
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.theme.BrandGradient
import com.mrdastbaaf.mdbapp.core.ui.theme.Ruby
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppPage
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.Hero
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Course

/**
 * The landing screen — the one that has to look like the product.
 *
 * The hero is the brand gradient with a drawn kilim lattice over it. A carpet
 * photograph was the obvious idea and the wrong one: it is busy exactly where
 * the headline needs quiet, it fights dark mode, and it would add a megabyte to
 * the APK for something a Canvas draws in a few hundred bytes.
 *
 * All copy comes from `GET /app-ui`, so the owner can change the headline, the
 * CTAs and the featured list without a release. The layout is fixed.
 */
@Composable
fun HomeScreen(
    state: UiState<HomeUiState>,
    isOffline: Boolean,
    onRetry: () -> Unit,
    onOpenCourse: (Int) -> Unit,
    onOpenPage: (String) -> Unit,
    onBrowseCourses: () -> Unit,
    onOpenMembership: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = stringResource(R.string.home_title),
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_retry),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        AsyncStateView(
            state = state,
            isOffline = isOffline,
            onRetry = onRetry,
            emptyMessage = stringResource(R.string.home_empty),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) { home ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.lg),
            ) {
                home.hero?.let { hero ->
                    item(key = "hero") {
                        Reveal(entered, index = 0) {
                            HeroCard(
                                hero = hero,
                                onPrimary = onBrowseCourses,
                                onSecondary = onOpenMembership,
                            )
                        }
                    }
                }

                if (home.featured.isNotEmpty()) {
                    item(key = "featured-title") {
                        Reveal(entered, index = 1) {
                            SectionHeader(
                                title = home.featuredTitle
                                    ?: stringResource(R.string.home_featured_fallback),
                                subtitle = home.featuredSubtitle,
                            )
                        }
                    }
                    item(key = "featured-row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            // Positional keys: the panel's curated id list can
                            // repeat a course, and a duplicate key is a crash.
                            itemsIndexed(items = home.featured) { _, course ->
                                FeaturedCourseCard(
                                    course = course,
                                    onClick = { course.productId?.let(onOpenCourse) },
                                )
                            }
                        }
                    }
                }

                if (home.pages.isNotEmpty()) {
                    item(key = "pages-title") {
                        Reveal(entered, index = 2) {
                            SectionHeader(
                                title = stringResource(R.string.home_pages_title),
                                subtitle = null,
                            )
                        }
                    }
                    item(key = "pages") {
                        Reveal(entered, index = 3) {
                            PagesCard(pages = home.pages, onOpenPage = onOpenPage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Reveal(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 300, delayMillis = index * 70)) +
            slideInVertically(tween(durationMillis = 380, delayMillis = index * 70)) { it / 12 },
    ) {
        content()
    }
}

// ── Hero ────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    hero: Hero,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .clip(RoundedCornerShape(28.dp))
            .background(BrandGradient),
    ) {
        // Texture, then depth. Both are drawn behind the content and neither
        // animates, so the hero costs one frame.
        KilimPattern(modifier = Modifier.matchParentSize(), alpha = 0.13f)

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.14f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (hero.badgeText.isNotBlank()) {
                Text(
                    text = hero.badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = Spacing.sm, vertical = 4.dp),
                )
            }

            Text(
                text = hero.headline,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp,
            )

            if (hero.subheadline.isNotBlank()) {
                Text(
                    text = hero.subheadline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 22.sp,
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            // Stacked, full width: side by side, a long Persian CTA collapses
            // into a one-letter-per-line sliver.
            Button(
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Ruby,
                ),
            ) {
                Text(
                    text = hero.ctaPrimaryText.ifBlank {
                        stringResource(R.string.home_cta_courses)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (hero.ctaSecondaryText.isNotBlank()) {
                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text(
                        text = hero.ctaSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (hero.stats.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    hero.stats.forEachIndexed { index, stat ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(Color.White.copy(alpha = 0.18f)),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = Spacing.sm),
                        ) {
                            Text(
                                text = PersianFormat.toPersianDigits(stat.value),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Sections ────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String?) {
    Column(
        modifier = Modifier.padding(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.md,
            bottom = Spacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun FeaturedCourseCard(course: Course, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(enabled = course.productId != null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                course.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (course.owned) {
                        stringResource(R.string.catalogue_owned)
                    } else {
                        PersianFormat.formatToman(course.priceToman)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PagesCard(pages: List<AppPage>, onOpenPage: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            pages.forEachIndexed { index, page ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPage(page.id) }
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                if (index != pages.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    )
                }
            }
        }
    }
}
