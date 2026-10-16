package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape scale.
 *
 * Slightly rounder than the M3 defaults: the brand is craft-oriented and the
 * softer corners read warmer against the cream background. The values stay on
 * the 4dp grid.
 */
val MdbShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Spacing scale. Material 3 uses an 8dp grid; these names exist so layout code
 * reads as intent rather than arithmetic.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** Minimum touch target, per the accessibility guidelines. */
    val minTouchTarget = 48.dp
}
