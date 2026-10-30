package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * The app theme.
 *
 * Three deliberate departures from a stock Compose setup:
 *
 *  1. **Dynamic colour is off.** Material You would repaint the app from the
 *     user's wallpaper, which breaks the brand identity the website establishes.
 *     A product that sells handicrafts needs its own palette, not the phone's.
 *
 *  2. **The layout direction is forced to RTL.** The app is Persian-only, so
 *     relying on the device locale to pick a direction would render the UI
 *     left-to-right on any phone set to English. Forcing it here means every
 *     screen is correct without a single `LocalLayoutDirection` override
 *     downstream — and it is one line to remove if an English build is ever
 *     added.
 *
 *  3. **The palette is a parameter.** [BrandPalette] defaults to the brand, but
 *     it can be swapped at runtime — which is what lets the owner recolour the
 *     app from the panel without shipping a new APK.
 */
@Composable
fun MdbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    brand: BrandPalette = BrandPalette.Default,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) brand.darkScheme else brand.lightScheme

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalBrand provides brand,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MdbTypography,
            shapes = MdbShapes,
            content = content,
        )
    }
}
