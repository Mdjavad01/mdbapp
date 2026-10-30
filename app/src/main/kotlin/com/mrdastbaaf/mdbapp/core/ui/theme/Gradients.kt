package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * The brand gradient, used wherever the app wants to look like itself rather
 * than like a form: the login header, an active membership, a highlighted plan.
 *
 * Composable, not a constant: the stops come from [LocalBrand], so a palette
 * change repaints every gradient in the app instead of leaving half of them on
 * the old colour. Always paired with white content — every stop is dark enough
 * for it.
 */
val BrandGradient: Brush
    @Composable get() = Brush.linearGradient(
        colors = LocalBrand.current.gradient,
        start = Offset.Zero,
        end = Offset.Infinite,
    )
