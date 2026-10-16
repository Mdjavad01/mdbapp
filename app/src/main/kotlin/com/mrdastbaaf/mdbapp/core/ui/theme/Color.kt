package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette, taken from the live PWA so the app and the website agree.
 *
 * The identity is warm: terracotta as the primary, amber as the accent, and a
 * cream page background rather than pure white. Persian handicrafts are the
 * product, so the palette leans craft rather than tech.
 */

// ── Brand anchors ───────────────────────────────────────────────────────────
val Terracotta = Color(0xFF9A3412)      // the PWA theme colour
val TerracottaDark = Color(0xFF7A2D0E)
val Amber = Color(0xFFD97706)           // the PWA accent / focus ring
val Cream = Color(0xFFFDFBF7)           // the PWA page background
val Ink = Color(0xFF1E293B)             // the PWA body text

// ── Light scheme ────────────────────────────────────────────────────────────
val LightPrimary = Terracotta
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFDBCF)
val LightOnPrimaryContainer = Color(0xFF3A0B00)

val LightSecondary = Color(0xFF77574C)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFDBCF)
val LightOnSecondaryContainer = Color(0xFF2C160E)

val LightTertiary = Color(0xFF6C5D2F)   // olive-gold: membership / premium cues
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFF5E1A7)
val LightOnTertiaryContainer = Color(0xFF231B00)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Cream
val LightOnBackground = Color(0xFF201A18)
val LightSurface = Cream
val LightOnSurface = Color(0xFF201A18)
val LightSurfaceVariant = Color(0xFFF5DED8)
val LightOnSurfaceVariant = Color(0xFF53433F)
val LightOutline = Color(0xFF85736E)
val LightOutlineVariant = Color(0xFFD8C2BC)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFFF8F6)
val LightSurfaceContainer = Color(0xFFFCEAE6)
val LightSurfaceContainerHigh = Color(0xFFF6E4E0)
val LightSurfaceContainerHighest = Color(0xFFF1DFDA)

// ── Dark scheme ─────────────────────────────────────────────────────────────
val DarkPrimary = Color(0xFFFFB59E)
val DarkOnPrimary = Color(0xFF5A1B02)
val DarkPrimaryContainer = Color(0xFF7A2D0E)
val DarkOnPrimaryContainer = Color(0xFFFFDBCF)

val DarkSecondary = Color(0xFFE7BEB1)
val DarkOnSecondary = Color(0xFF442A20)
val DarkSecondaryContainer = Color(0xFF5D4036)
val DarkOnSecondaryContainer = Color(0xFFFFDBCF)

val DarkTertiary = Color(0xFFD8C58D)
val DarkOnTertiary = Color(0xFF3B2F05)
val DarkTertiaryContainer = Color(0xFF53461A)
val DarkOnTertiaryContainer = Color(0xFFF5E1A7)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF1A110F)
val DarkOnBackground = Color(0xFFF1DFDA)
val DarkSurface = Color(0xFF1A110F)
val DarkOnSurface = Color(0xFFF1DFDA)
val DarkSurfaceVariant = Color(0xFF53433F)
val DarkOnSurfaceVariant = Color(0xFFD8C2BC)
val DarkOutline = Color(0xFFA08C87)
val DarkOutlineVariant = Color(0xFF53433F)

val DarkSurfaceContainerLowest = Color(0xFF140C0A)
val DarkSurfaceContainerLow = Color(0xFF221917)
val DarkSurfaceContainer = Color(0xFF271D1B)
val DarkSurfaceContainerHigh = Color(0xFF322825)
val DarkSurfaceContainerHighest = Color(0xFF3D3230)
