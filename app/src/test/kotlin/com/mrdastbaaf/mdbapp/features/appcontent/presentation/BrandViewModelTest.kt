package com.mrdastbaaf.mdbapp.features.appcontent.presentation

import com.mrdastbaaf.mdbapp.core.ui.theme.BrandPalette
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppTheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Hex from the panel → palette.
 *
 * The case that matters is a half-bad payload: the owner sets one colour and
 * leaves the rest, or a plugin update writes something that is not a hex. Each
 * anchor falls back on its own, so the app never ends up painted with one new
 * colour and three unrelated ones.
 */
class BrandViewModelTest {

    private val default = BrandPalette.Default

    @Test
    fun `a full palette replaces all four anchors`() {
        val palette = AppTheme(
            primary = "#123456",
            accent = "#ABCDEF",
            canvas = "#FFFFFF",
            success = "#00AA00",
        ).toPalette()

        assertEquals("#123456", palette.primary.toHex())
        assertEquals("#ABCDEF", palette.accent.toHex())
        assertEquals("#FFFFFF", palette.canvas.toHex())
        assertEquals("#00AA00", palette.success.toHex())
    }

    @Test
    fun `a blank or missing anchor keeps its default`() {
        val palette = AppTheme(
            primary = null,
            accent = "",
            canvas = "   ",
            success = null,
        ).toPalette()

        assertEquals("#9A3412", palette.primary.toHex())
        assertEquals("#D97706", palette.accent.toHex())
        assertEquals("#FDFBF7", palette.canvas.toHex())
        assertEquals("#047857", palette.success.toHex())
    }

    @Test
    fun `an invalid hex falls back without touching the other anchors`() {
        val palette = AppTheme(
            primary = "red",
            accent = "#12345",     // five digits — not a colour
            canvas = "#FDFBF7",
            success = "#00AA00",
        ).toPalette()

        // Two bad anchors fall back, the two good ones survive.
        assertEquals(default.primary.toHex(), palette.primary.toHex())
        assertEquals(default.accent.toHex(), palette.accent.toHex())
        assertEquals("#FDFBF7", palette.canvas.toHex())
        assertEquals("#00AA00", palette.success.toHex())
    }

    // roundToInt, not toInt: 154/255f * 255f is 153.99999, and truncation would
    // make every assertion fail for the wrong reason.
    private fun Color.toHex(): String {
        val r = (red * 255f).roundToInt()
        val g = (green * 255f).roundToInt()
        val b = (blue * 255f).roundToInt()
        return "#%02X%02X%02X".format(r, g, b)
    }
}
