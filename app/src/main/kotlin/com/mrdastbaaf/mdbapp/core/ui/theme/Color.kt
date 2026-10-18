package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette.
 *
 * The identity is a saturated ruby rather than the flat terracotta the PWA uses
 * for its chrome: on a phone the primary colour is the app's face, not a header
 * stripe, and terracotta read as brown once Material 3 toned it. Gold stays as
 * the accent, because it is what makes a "premium" membership legible without
 * introducing a second hue.
 */

// ── Brand anchors ───────────────────────────────────────────────────────────
val Ruby = Color(0xFFC2183B)            // primary
val RubyBright = Color(0xFFE11D48)      // gradient start
val RubyDeep = Color(0xFF8C0A24)        // gradient end
val Gold = Color(0xFFB07A00)
val Cream = Color(0xFFFFF8F6)
val Ink = Color(0xFF211014)

/** The brand gradient. White text is legible across its whole range. */
val BrandGradientColors = listOf(RubyBright, Ruby, RubyDeep)

// ── Light scheme ────────────────────────────────────────────────────────────
val LightPrimary = Ruby
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFD9DE)
val LightOnPrimaryContainer = Color(0xFF40000E)

val LightSecondary = Color(0xFF7D5260)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFD9E2)
val LightOnSecondaryContainer = Color(0xFF31101D)

val LightTertiary = Gold
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFE08A)
val LightOnTertiaryContainer = Color(0xFF261A00)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Cream
val LightOnBackground = Ink
val LightSurface = Cream
val LightOnSurface = Ink
val LightSurfaceVariant = Color(0xFFF7DDE1)
val LightOnSurfaceVariant = Color(0xFF564345)
val LightOutline = Color(0xFF8A7074)
val LightOutlineVariant = Color(0xFFDCBFC4)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFFF5F6)
val LightSurfaceContainer = Color(0xFFFDEBED)
val LightSurfaceContainerHigh = Color(0xFFF7E5E7)
val LightSurfaceContainerHighest = Color(0xFFF1DFE1)

// ── Dark scheme ─────────────────────────────────────────────────────────────
val DarkPrimary = Color(0xFFFFB2BF)
val DarkOnPrimary = Color(0xFF68001C)
val DarkPrimaryContainer = Color(0xFF8C0A24)
val DarkOnPrimaryContainer = Color(0xFFFFD9DE)

val DarkSecondary = Color(0xFFE7BDC8)
val DarkOnSecondary = Color(0xFF462632)
val DarkSecondaryContainer = Color(0xFF5F3C49)
val DarkOnSecondaryContainer = Color(0xFFFFD9E2)

val DarkTertiary = Color(0xFFE6C349)
val DarkOnTertiary = Color(0xFF3D2E00)
val DarkTertiaryContainer = Color(0xFF574400)
val DarkOnTertiaryContainer = Color(0xFFFFE08A)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF150A0D)
val DarkOnBackground = Color(0xFFF5DDE0)
val DarkSurface = Color(0xFF150A0D)
val DarkOnSurface = Color(0xFFF5DDE0)
val DarkSurfaceVariant = Color(0xFF564345)
val DarkOnSurfaceVariant = Color(0xFFDCBFC4)
val DarkOutline = Color(0xFFA48B90)
val DarkOutlineVariant = Color(0xFF564345)

val DarkSurfaceContainerLowest = Color(0xFF100609)
val DarkSurfaceContainerLow = Color(0xFF1E1114)
val DarkSurfaceContainer = Color(0xFF231519)
val DarkSurfaceContainerHigh = Color(0xFF2E2024)
val DarkSurfaceContainerHighest = Color(0xFF392A2E)
