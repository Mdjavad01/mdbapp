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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.mrdastbaaf.mdbapp.core.ui.component.BrandTopAppBar
import com.mrdastbaaf.mdbapp.core.ui.component.GradientButton
import com.mrdastbaaf.mdbapp.core.ui.component.KilimPattern
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.state.dataOrNull
import com.mrdastbaaf.mdbapp.core.ui.theme.BrandGradient
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.ExternalLinks
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Course
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.CourseDetail
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Lesson

/**
 * One course: hero, description and the lesson list.
 *
 * The lock on a lesson row is **advisory**. Whether a lesson can actually be
 * played is decided by the server when a playback ticket is requested, and this
 * screen never gates anything by itself — it only avoids offering a tap that
 * would be refused.
 */
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
            BrandTopAppBar(
                title = state.dataOrNull?.course?.title
                    ?: stringResource(R.string.course_detail_title),
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
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item(key = "hero") { Hero(detail.course) }

                item(key = "header") {
                    CourseHeader(
                        detail = detail,
                        onStartCourse = {
                            detail.lessons.firstOrNull()?.let { lesson ->
                                onPlayLesson(lesson.id)
                            }
                        },
                    )
                }

                if (detail.course.description.isNotBlank()) {
                    item(key = "description") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ) {
                            Text(
                                text = detail.course.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(Spacing.md),
                            )
                        }
                    }
                }

                item(key = "lessons-title") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.padding(
                            start = Spacing.md,
                            end = Spacing.md,
                            top = Spacing.md,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = 16.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = stringResource(
                                R.string.course_detail_lessons,
                                PersianFormat.formatNumber(detail.lessonCount.toLong()),
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (detail.lessons.isEmpty()) {
                    item(key = "no-lessons") {
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
                    LessonCard(
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
                }

                if (!detail.course.owned) {
                    item(key = "access-note") {
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

// ── Hero ────────────────────────────────────────────────────────────────────

@Composable
private fun Hero(course: Course) {
    val imageUrl = course.imageUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // No thumbnail: the brand surface instead of a grey rectangle.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandGradient),
            ) {
                KilimPattern(modifier = Modifier.matchParentSize(), alpha = 0.14f)
            }
        }

        // Scrim, so the chip stays legible over any photo.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                ),
        )

        Text(
            text = when {
                course.owned -> stringResource(R.string.catalogue_owned)
                course.priceToman == 0L -> stringResource(R.string.catalogue_free)
                else -> PersianFormat.formatToman(course.priceToman)
            },
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.md)
                .clip(RoundedCornerShape(50))
                .background(
                    if (course.owned) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                    } else {
                        Color.Black.copy(alpha = 0.45f)
                    },
                )
                .padding(horizontal = Spacing.md, vertical = 6.dp),
        )
    }
}

// ── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun CourseHeader(detail: CourseDetail, onStartCourse: () -> Unit) {
    val course = detail.course

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = course.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            course.level?.let { MetaChip(it) }
            if (course.durationMinutes > 0) {
                MetaChip(PersianFormat.formatDurationMinutes(course.durationMinutes))
            }
            MetaChip(
                stringResource(
                    R.string.catalogue_lessons_count,
                    PersianFormat.formatNumber(detail.lessonCount.toLong()),
                ),
            )
        }

        if (course.owned && detail.lessons.isNotEmpty()) {
            GradientButton(
                text = stringResource(R.string.course_detail_start),
                onClick = onStartCourse,
            )
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
    )
}

// ── Lessons ─────────────────────────────────────────────────────────────────

@Composable
private fun LessonCard(
    index: Int,
    lesson: Lesson,
    courseOwned: Boolean,
    onClick: (() -> Unit)?,
) {
    val locked = !courseOwned && !lesson.isFreePreview

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (locked) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = PersianFormat.formatNumber(index.toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (locked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
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
                    modifier = Modifier.size(18.dp),
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
}

@Composable
private fun AccessNote(onOpenMembership: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md)
            .clip(RoundedCornerShape(20.dp))
            .background(BrandGradient)
            .padding(Spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.course_detail_access_note),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
            GradientButton(
                text = stringResource(R.string.access_renew),
                onClick = onOpenMembership,
            )
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
