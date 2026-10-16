package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mrdastbaaf.mdbapp.R

/**
 * Vazirmatn — the same typeface the web app ships, bundled as a resource rather
 * than fetched at runtime so first paint never waits on the network and the
 * rendering matches the website exactly.
 */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

/**
 * Persian text has shallower ascenders and deeper descenders than Latin, and the
 * Material defaults are tuned for Latin. Running text therefore gets a taller
 * line height so diacritics and the Persian descenders (ج چ ح خ) are not crowded.
 */
private const val RUNNING_TEXT_LINE_HEIGHT_MULTIPLIER = 1.6f

private fun TextStyle.persian(lineHeightMultiplier: Float = RUNNING_TEXT_LINE_HEIGHT_MULTIPLIER) =
    copy(
        fontFamily = Vazirmatn,
        lineHeight = fontSize * lineHeightMultiplier,
    )

/** Material 3 type scale, restyled for Persian. */
private val base = Typography()

val MdbTypography = Typography(
    displayLarge = base.displayLarge.persian(1.25f),
    displayMedium = base.displayMedium.persian(1.25f),
    displaySmall = base.displaySmall.persian(1.25f),

    headlineLarge = base.headlineLarge.persian(1.35f),
    headlineMedium = base.headlineMedium.persian(1.35f),
    headlineSmall = base.headlineSmall.persian(1.4f),

    titleLarge = base.titleLarge.persian(1.45f),
    titleMedium = base.titleMedium.persian(1.5f),
    titleSmall = base.titleSmall.persian(1.5f),

    bodyLarge = base.bodyLarge.persian(),
    bodyMedium = base.bodyMedium.persian(),
    bodySmall = base.bodySmall.persian(1.55f),

    labelLarge = base.labelLarge.persian(1.45f),
    labelMedium = base.labelMedium.persian(1.45f),
    labelSmall = base.labelSmall.persian(1.45f),
)

/**
 * Numeric text — prices, durations, countdowns.
 *
 * Kept separate because numbers are the one place Persian typography needs
 * extra care: they are rendered with Persian-Indic digits (see
 * `core/util/PersianFormat.kt`) and read better with no letter spacing.
 */
val NumericStyle = TextStyle(
    fontFamily = Vazirmatn,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    letterSpacing = 0.sp,
    lineHeight = 24.sp,
)
