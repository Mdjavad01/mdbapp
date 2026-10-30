package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The brand, as four anchors.
 *
 * Everything else — surfaces, containers, outlines, the gradient, the colour
 * that sits on top of the primary — is **derived** from these four. That is the
 * point: the palette gets edited once a year, and a file of forty hand-picked
 * hex values is forty chances to leave one behind and end up with a stray pink
 * card on a brown app.
 *
 * To rebrand, change [primary] (and optionally [accent] / [canvas]). Nothing
 * else in the codebase names a colour.
 *
 * The defaults are sampled from mrdastbaaf.com itself — the PWA's
 * `theme-color` and the bridge's admin CSS — so the app and the site are the
 * same brand rather than two interpretations of it.
 */
data class BrandPalette(
    val primary: Color,
    val accent: Color,
    val canvas: Color,
    val success: Color,
) {
    /**
     * Start → primary → end, for hero headers and highlighted cards.
     *
     * Three stops, not two: a two-stop ramp between one colour and a shade of
     * itself reads as a flat block with a gradient slapped on. The lighter
     * leading stop gives it a direction.
     */
    val gradient: List<Color>
        get() = listOf(lighten(primary, 0.14f), primary, darken(primary, 0.18f))

    /** Warm near-black, used as body text on the cream canvas. */
    private val ink: Color get() = darken(primary, 0.86f)

    val lightScheme: ColorScheme
        get() = lightColorScheme(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = lighten(primary, 0.86f),
            onPrimaryContainer = darken(primary, 0.78f),
            secondary = mute(darken(primary, 0.20f), 0.45f),
            onSecondary = Color.White,
            secondaryContainer = lighten(accent, 0.80f),
            onSecondaryContainer = darken(accent, 0.72f),
            tertiary = accent,
            onTertiary = readableOn(accent),
            tertiaryContainer = lighten(accent, 0.78f),
            onTertiaryContainer = darken(accent, 0.70f),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            background = canvas,
            onBackground = ink,
            surface = canvas,
            onSurface = ink,
            surfaceVariant = mix(canvas, primary, 0.10f),
            // Secondary text has to survive being read on the cream canvas at
            // 12sp, so the derivation is contrast-checked rather than trusted.
            onSurfaceVariant = ensureContrast(mute(darken(primary, 0.42f), 0.35f), canvas, 4.5f),
            outline = mute(darken(primary, 0.16f), 0.40f),
            outlineVariant = mix(canvas, primary, 0.18f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = mix(canvas, primary, 0.03f),
            surfaceContainer = mix(canvas, primary, 0.06f),
            surfaceContainerHigh = mix(canvas, primary, 0.09f),
            surfaceContainerHighest = mix(canvas, primary, 0.12f),
        )

    val darkScheme: ColorScheme
        get() {
            val surface = darken(primary, 0.90f)
            val onSurface = mix(canvas, primary, 0.10f)
            return darkColorScheme(
                // On a near-black surface the primary has to be lifted, not
                // reused: the brand terracotta at full strength is unreadable
                // as a label at night.
                primary = ensureContrast(lighten(primary, 0.55f), surface, 4.5f),
                onPrimary = darken(primary, 0.80f),
                primaryContainer = darken(primary, 0.35f),
                onPrimaryContainer = lighten(primary, 0.88f),
                secondary = lighten(mute(primary, 0.45f), 0.55f),
                onSecondary = darken(primary, 0.78f),
                secondaryContainer = darken(primary, 0.30f),
                onSecondaryContainer = lighten(accent, 0.85f),
                tertiary = lighten(accent, 0.30f),
                onTertiary = darken(accent, 0.75f),
                tertiaryContainer = darken(accent, 0.55f),
                onTertiaryContainer = lighten(accent, 0.85f),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6),
                background = surface,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = darken(primary, 0.72f),
                onSurfaceVariant = mix(onSurface, surface, 0.35f),
                outline = mix(onSurface, surface, 0.55f),
                outlineVariant = darken(primary, 0.78f),
                surfaceContainerLowest = darken(primary, 0.94f),
                surfaceContainerLow = darken(primary, 0.88f),
                surfaceContainer = darken(primary, 0.85f),
                surfaceContainerHigh = darken(primary, 0.80f),
                surfaceContainerHighest = darken(primary, 0.74f),
            )
        }

    companion object {
        // ── The four anchors. Change these, and the app changes. ─────────────
        val Terracotta = Color(0xFF9A3412)
        val Saffron = Color(0xFFD97706)
        val Cream = Color(0xFFFDFBF7)
        val Emerald = Color(0xFF047857)

        val Default = BrandPalette(
            primary = Terracotta,
            accent = Saffron,
            canvas = Cream,
            success = Emerald,
        )

        /**
         * Reads `#RRGGBB` or `RRGGBB`. Returns null for anything else, so a bad
         * value from the panel falls back to the default instead of painting
         * the app black.
         */
        fun parseHex(value: String?): Color? {
            val raw = value?.trim()?.removePrefix("#") ?: return null
            if (raw.length != 6 || raw.any { it.lowercaseChar() !in "0123456789abcdef" }) return null
            val packed = raw.toLongOrNull(16) ?: return null
            return Color(0xFF000000 or packed)
        }
    }
}

/**
 * The active palette.
 *
 * A `staticCompositionLocalOf` because it is read in nearly every composable and
 * changes roughly never — a dynamic local would invalidate the whole tree on
 * every read for a value that is constant per session.
 */
val LocalBrand = staticCompositionLocalOf { BrandPalette.Default }
