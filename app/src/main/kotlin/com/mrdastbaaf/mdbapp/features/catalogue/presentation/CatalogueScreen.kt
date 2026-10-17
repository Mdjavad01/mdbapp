package com.mrdastbaaf.mdbapp.features.catalogue.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.component.AsyncStateView
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Course

/**
 * The catalogue tab.
 *
 * Written as a pure function of its state so it can be previewed without Hilt —
 * the route composable in the navigation graph is what injects the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    state: CatalogueUiState,
    isOffline: Boolean,
    onModeSelected: (CatalogueMode) -> Unit,
    onRetry: () -> Unit,
    onOpenCourse: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalogue_title)) },
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_retry),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) {
            ModeSelector(
                selected = state.mode,
                onSelect = onModeSelected,
            )

            AsyncStateView(
                state = state.courses,
                // Without the weight the Box inside AsyncStateView is measured
                // with an unbounded height inside this Column and collapses.
                modifier = Modifier.weight(1f),
                isOffline = isOffline,
                onRetry = onRetry,
                emptyMessage = when (state.mode) {
                    CatalogueMode.ALL -> stringResource(R.string.catalogue_empty_all)
                    CatalogueMode.MINE -> stringResource(R.string.catalogue_empty_mine)
                },
            ) { courses ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.sm,
                        bottom = Spacing.lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(items = courses, key = { it.id }) { course ->
                        CourseCard(
                            course = course,
                            // No product id means the detail route cannot be
                            // built; better a card that does not respond than a
                            // tap that lands on a 404.
                            onClick = course.productId?.let { id -> { onOpenCourse(id) } },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selected: CatalogueMode,
    onSelect: (CatalogueMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FilterChip(
            selected = selected == CatalogueMode.ALL,
            onClick = { onSelect(CatalogueMode.ALL) },
            label = { Text(stringResource(R.string.catalogue_tab_all)) },
        )
        FilterChip(
            selected = selected == CatalogueMode.MINE,
            onClick = { onSelect(CatalogueMode.MINE) },
            label = { Text(stringResource(R.string.catalogue_tab_mine)) },
        )
    }
}

@Composable
private fun CourseCard(course: Course, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CourseThumbnail(course)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val meta = courseMeta(course)
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(Spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PriceLabel(course)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(
                            R.string.catalogue_lessons_count,
                            PersianFormat.formatNumber(course.lessonsCount.toLong()),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseThumbnail(course: Course) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = course.imageUrl
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = stringResource(R.string.catalogue_placeholder),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PriceLabel(course: Course) {
    val text: String
    val color = when {
        course.owned -> {
            text = stringResource(R.string.catalogue_owned)
            MaterialTheme.colorScheme.primary
        }

        course.priceToman == 0L -> {
            text = stringResource(R.string.catalogue_free)
            MaterialTheme.colorScheme.tertiary
        }

        else -> {
            text = PersianFormat.formatToman(course.priceToman)
            MaterialTheme.colorScheme.onSurface
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.SemiBold,
    )
}

/** `مقدماتی • ۸ ساعت` — whichever parts the server actually supplied. */
private fun courseMeta(course: Course): String = buildList {
    course.level?.let(::add)
    course.category?.let(::add)
    if (course.durationMinutes > 0) {
        add(PersianFormat.formatDurationMinutes(course.durationMinutes))
    }
}.joinToString(separator = " • ")

// ── Previews ────────────────────────────────────────────────────────────────

private val PreviewCourses = listOf(
    Course(
        id = "gelim-bafi",
        productId = 123,
        title = "گلیم‌بافی مقدماتی — از نخ تا نقشه",
        description = "دورهٔ آغازین گلیم‌بافی",
        imageUrl = null,
        category = "دوره‌های آموزشی",
        level = "مقدماتی",
        lessonsCount = 24,
        durationMinutes = 480,
        priceToman = 1_850_000,
        freePreviewCount = 3,
        owned = false,
    ),
    Course(
        id = "serme-bafi",
        productId = 124,
        title = "سرمه‌بافی پیشرفته",
        description = "دورهٔ پیشرفته",
        imageUrl = null,
        category = "دوره‌های آموزشی",
        level = "پیشرفته",
        lessonsCount = 18,
        durationMinutes = 360,
        priceToman = 0,
        freePreviewCount = 2,
        owned = true,
    ),
)

@Preview(name = "کاتالوگ", showBackground = true, locale = "fa")
@Composable
private fun CataloguePreview() {
    MdbTheme {
        CatalogueScreen(
            state = CatalogueUiState(
                mode = CatalogueMode.ALL,
                courses = UiState.Success(PreviewCourses),
            ),
            isOffline = false,
            onModeSelected = {},
            onRetry = {},
            onOpenCourse = {},
        )
    }
}
