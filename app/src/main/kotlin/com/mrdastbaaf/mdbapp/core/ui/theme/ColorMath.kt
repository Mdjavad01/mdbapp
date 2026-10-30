package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Small colour arithmetic, so the palette can be derived instead of dictated.
 *
 * Everything here works in sRGB and ignores alpha: brand colours are always
 * opaque, and blending a translucent one with white in the wrong order is how
 * "the button looked fine in Figma" happens.
 */

/** `t = 0` returns [a], `t = 1` returns [b]. */
internal fun mix(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = 1f,
    )
}

internal fun darken(color: Color, amount: Float): Color = mix(color, Color.Black, amount)

internal fun lighten(color: Color, amount: Float): Color = mix(color, Color.White, amount)

/**
 * Pulls a colour toward its own grey.
 *
 * Used for secondary text and outlines: a saturated terracotta at 40% lightness
 * is still unmistakably orange, which is wrong for a caption but right for a
 * button.
 */
internal fun mute(color: Color, amount: Float): Color {
    val luma = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    return mix(color, Color(luma, luma, luma, 1f), amount)
}

/**
 * WCAG 2.1 relative luminance — gamma-decoded, not a plain channel average.
 *
 * The distinction matters: a mid orange and a mid blue have the same average
 * and wildly different luminance, and only one of them can carry white text.
 */
internal fun relativeLuminance(color: Color): Float {
    fun channel(value: Float): Float {
        val c = value.coerceIn(0f, 1f)
        return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(color.red) +
        0.7152f * channel(color.green) +
        0.0722f * channel(color.blue)
}

/** 1.0 (identical) to 21.0 (black on white). 4.5 is the AA threshold for text. */
internal fun contrastRatio(a: Color, b: Color): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/** White or near-black, whichever reads better on [background]. */
internal fun readableOn(
    background: Color,
    dark: Color = Color(0xFF150702),
    light: Color = Color.White,
): Color = if (contrastRatio(background, light) >= contrastRatio(background, dark)) light else dark

/**
 * Pushes [foreground] toward black or white until it clears [minimum] on
 * [background].
 *
 * This is the guard for a palette the owner can edit: if someone sets the
 * primary to a pale saffron, white-on-primary text would vanish. Here it turns
 * dark instead of becoming invisible.
 */
internal fun ensureContrast(foreground: Color, background: Color, minimum: Float = 4.5f): Color {
    if (contrastRatio(foreground, background) >= minimum) return foreground
    val target = if (relativeLuminance(background) > 0.5f) Color.Black else Color.White
    var candidate = foreground
    for (step in 1..20) {
        candidate = mix(foreground, target, step / 20f)
        if (contrastRatio(candidate, background) >= minimum) return candidate
    }
    return target
}
