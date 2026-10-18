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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.component.AsyncStateView
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.state.dataOrNull
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.ExternalLinks
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Course
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.CourseDetail
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Lesson

/**
 * One course: header, description and the lesson list.
 *
 * The lock on a lesson row is **advisory**. Whether a lesson can actually be
 * played is decided by the server when a playback ticket is requested, and this
 * screen never gates anything by itself — it only avoids offering a tap that
 * would be refused.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    state: UiState<CourseDetail>,
    isOffline: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayLesson: (String) -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.dataOrNull?.course?.title
                            ?: stringResource(R.string.course_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        AsyncStateView(
            state = state,
            isOffline = isOffline,
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) { detail ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.xl),
            ) {
                item { Hero(detail.course) }

                item { CourseHeader(detail) }

                if (detail.course.description.isNotBlank()) {
                    item {
                        Text(
                            text = detail.course.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.md),
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(
                            R.string.course_detail_lessons,
                            PersianFormat.formatNumber(detail.lessonCount.toLong()),
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = Spacing.md,
                            end = Spacing.md,
                            top = Spacing.lg,
                            bottom = Spacing.xs,
                        ),
                    )
                }

                if (detail.lessons.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.course_detail_no_lessons),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.md),
                        )
                    }
                }

                itemsIndexed(
                    items = detail.lessons,
                    key = { _, lesson -> lesson.id },
                ) { index, lesson ->
                    LessonRow(
                        index = index + 1,
                        lesson = lesson,
                        courseOwned = detail.course.owned,
                        // The lock is advisory: tapping an unlocked lesson opens
                        // the player, and the server still decides access when
                        // the ticket is requested. A locked row has no tap, so
                        // it cannot lead to a refusal.
                        onClick = if (detail.course.owned || lesson.isFreePreview) {
                            { onPlayLesson(lesson.id) }
                        } else {
                            null
                        },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )
                }

                if (!detail.course.owned) {
                    item {
                        AccessNote(
                            onOpenMembership = {
                                ExternalLinks.open(context, ExternalLinks.MEMBERSHIP_URL)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(course: Course) {
    val imageUrl = course.imageUrl

    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.catalogue_placeholder),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CourseHeader(detail: CourseDetail) {
    val course = detail.course

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = course.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val meta = buildList {
            course.level?.let(::add)
            course.category?.let(::add)
            if (course.durationMinutes > 0) {
                add(PersianFormat.formatDurationMinutes(course.durationMinutes))
            }
        }

        if (meta.isNotEmpty()) {
            Text(
                text = meta.joinToString(separator = " • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.xs))

        Text(
            text = when {
                course.owned -> stringResource(R.string.catalogue_owned)
                course.priceToman == 0L -> stringResource(R.string.catalogue_free)
                else -> PersianFormat.formatToman(course.priceToman)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (course.owned) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun LessonRow(
    index: Int,
    lesson: Lesson,
    courseOwned: Boolean,
    onClick: (() -> Unit)?,
) {
    val locked = !courseOwned && !lesson.isFreePreview

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = PersianFormat.formatNumber(index.toLong()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // `stringResource` is @Composable and `buildList` takes a plain
            // lambda, so the label has to be resolved before the builder.
            val freePreviewLabel = stringResource(R.string.course_detail_free_preview)
            val subtitle = buildList {
                lesson.durationMinutes?.let {
                    add(PersianFormat.formatDurationMinutes(it))
                }
                if (lesson.isFreePreview) {
                    add(freePreviewLabel)
                }
            }.joinToString(separator = " • ")

            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lesson.isFreePreview) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        if (locked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.course_detail_locked),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.player_title),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AccessNote(onOpenMembership: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.course_detail_access_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onOpenMembership) {
                Text(stringResource(R.string.access_renew))
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@Preview(name = "جزئیات دوره", showBackground = true, locale = "fa")
@Composable
private fun CourseDetailPreview() {
    MdbTheme {
        CourseDetailScreen(
            state = UiState.Success(
                CourseDetail(
                    course = Course(
                        id = "gelim-bafi",
                        productId = 123,
                        title = "گلیم‌بافی مقدماتی — از نخ تا نقشه",
                        description = "در این دوره از صفر با ابزار، نخ و نقشهٔ گلیم آشنا می‌شوید.",
                        imageUrl = null,
                        category = "دوره‌های آموزشی",
                        level = "مقدماتی",
                        lessonsCount = 3,
                        durationMinutes = 480,
                        priceToman = 1_850_000,
                        freePreviewCount = 1,
                        owned = false,
                    ),
                    lessons = listOf(
                        Lesson("1", "جلسهٔ اول — معرفی ابزار", true, MembershipTier.BRONZE, 22),
                        Lesson("2", "جلسهٔ دوم — انتخاب نخ", false, MembershipTier.BRONZE, 31),
                        Lesson("3", "جلسهٔ سوم — نقشه‌خوانی", false, MembershipTier.SILVER, null),
                    ),
                ),
            ),
            isOffline = false,
            onBack = {},
            onRetry = {},
            onPlayLesson = {},
        )
    }
}
