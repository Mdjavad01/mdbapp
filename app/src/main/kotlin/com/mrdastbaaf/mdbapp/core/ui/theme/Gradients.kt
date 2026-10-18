package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * The brand gradient, used wherever the app wants to look like itself rather
 * than like a form: the login header, an active membership, a highlighted plan.
 *
 * Always paired with white content — every stop is dark enough for it.
 */
val BrandGradient: Brush = Brush.linearGradient(
    colors = BrandGradientColors,
    start = Offset.Zero,
    end = Offset.Infinite,
)
