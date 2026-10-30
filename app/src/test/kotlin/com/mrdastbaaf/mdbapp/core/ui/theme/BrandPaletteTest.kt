package com.mrdastbaaf.mdbapp.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * The palette is derived, so these tests guard the derivation rather than the
 * hex values. A derived scheme that quietly produces grey-on-grey is worse than
 * a hardcoded one, because nobody can see it in a diff.
 */
class BrandPaletteTest {

    private val brand = BrandPalette.Default

    @Test
    fun `default anchors are the site palette`() {
        assertEquals("#9A3412", brand.primary.toHex())
        assertEquals("#D97706", brand.accent.toHex())
        assertEquals("#FDFBF7", brand.canvas.toHex())
    }

    @Test
    fun `light scheme is readable`() {
        val scheme = brand.lightScheme
        assertTrue(
            "primary needs 4.5:1 for its own label",
            contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f,
        )
        assertTrue(
            "body text needs 7:1",
            contrastRatio(scheme.onSurface, scheme.surface) >= 7f,
        )
        assertTrue(
            "secondary text needs 4.5:1",
            contrastRatio(scheme.onSurfaceVariant, scheme.surface) >= 4.5f,
        )
        assertTrue(
            "primary label on its container",
            contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5f,
        )
    }

    @Test
    fun `dark scheme is readable`() {
        val scheme = brand.darkScheme
        assertTrue(
            "primary label needs 4.5:1 on a near-black surface",
            contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f,
        )
        assertTrue(
            "body text needs 7:1",
            contrastRatio(scheme.onSurface, scheme.surface) >= 7f,
        )
        assertTrue(
            "secondary text needs 4.5:1",
            contrastRatio(scheme.onSurfaceVariant, scheme.surface) >= 4.5f,
        )
    }

    @Test
    fun `gradient runs light to dark around the primary`() {
        val gradient = brand.gradient
        assertEquals(3, gradient.size)
        assertEquals(brand.primary, gradient[1])
        assertTrue(
            "the leading stop must be lighter than the trailing one",
            relativeLuminance(gradient[0]) > relativeLuminance(gradient[2]),
        )
    }

    @Test
    fun `a pale primary still gets a dark label`() {
        // Saffron at full strength cannot carry white text; the scheme must
        // notice that instead of drawing an invisible button.
        val pale = brand.copy(primary = Color(0xFFF5C77E))
        assertTrue(contrastRatio(pale.lightScheme.onPrimary, pale.primary) >= 4.5f)
        assertEquals(pale.lightScheme.onPrimary, readableOn(pale.primary))
    }

    @Test
    fun `changing one anchor changes the whole scheme`() {
        val navy = brand.copy(primary = Color(0xFF1E293B))
        assertTrue(
            "surfaces follow the primary",
            navy.lightScheme.surfaceContainer != brand.lightScheme.surfaceContainer,
        )
        assertTrue(
            "the gradient follows the primary",
            navy.gradient[1] != brand.gradient[1],
        )
    }

    @Test
    fun `hex parsing accepts what a panel would send and rejects the rest`() {
        assertEquals(Color(0xFF9A3412), BrandPalette.parseHex("#9A3412"))
        assertEquals(Color(0xFF9A3412), BrandPalette.parseHex("9a3412"))
        assertEquals(Color(0xFF9A3412), BrandPalette.parseHex("  #9A3412 "))
        assertNotNull(BrandPalette.parseHex("D97706"))
        assertNull(BrandPalette.parseHex(null))
        assertNull(BrandPalette.parseHex(""))
        assertNull(BrandPalette.parseHex("red"))
        assertNull(BrandPalette.parseHex("#GGGGGG"))
        assertNull(BrandPalette.parseHex("#12345"))
    }

    private fun Color.toHex(): String {
        // roundToInt, not toInt: 154/255f * 255f is 153.99999, and truncation
        // would make every one of these assertions fail for the wrong reason.
        val r = (red * 255f).roundToInt()
        val g = (green * 255f).roundToInt()
        val b = (blue * 255f).roundToInt()
        return "#%02X%02X%02X".format(r, g, b)
    }
}
