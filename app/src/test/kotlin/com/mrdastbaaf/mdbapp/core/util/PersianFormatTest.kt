package com.mrdastbaaf.mdbapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The formatting boundary between the API and the screen.
 *
 * The server speaks ASCII and the UI speaks Persian, and mixing the two is the
 * single most likely source of a bug that only shows up in production — a phone
 * number typed with a Persian keyboard, or a price rendered with Latin digits
 * next to Persian text.
 */
class PersianFormatTest {

    @Test
    fun `converts ascii digits to persian`() {
        assertEquals("۰۱۲۳۴۵۶۷۸۹", PersianFormat.toPersianDigits("0123456789"))
    }

    @Test
    fun `leaves non digits untouched`() {
        assertEquals("۰۹۱۲ تومان", PersianFormat.toPersianDigits("0912 تومان"))
    }

    @Test
    fun `converts persian digits back to ascii`() {
        assertEquals("0123456789", PersianFormat.toAsciiDigits("۰۱۲۳۴۵۶۷۸۹"))
    }

    @Test
    fun `converts arabic indic digits back to ascii`() {
        assertEquals("0123456789", PersianFormat.toAsciiDigits("٠١٢٣٤٥٦٧٨٩"))
    }

    @Test
    fun `round trips digits through both directions`() {
        val ascii = "09123456789"
        assertEquals(ascii, PersianFormat.toAsciiDigits(PersianFormat.toPersianDigits(ascii)))
    }

    @Test
    fun `formats a toman amount with persian grouping`() {
        // The Persian locale groups with U+066C (٬), not a Latin comma.
        val formatted = PersianFormat.formatToman(490_000)
        assertEquals("۴۹۰٬۰۰۰ تومان", formatted)
    }

    @Test
    fun `formats a small amount without a separator`() {
        assertEquals("۹۵۰ تومان", PersianFormat.formatToman(950))
    }

    @Test
    fun `formats a duration with hours and minutes`() {
        assertEquals("۲ ساعت و ۳۰ دقیقه", PersianFormat.formatDurationMinutes(150))
    }

    @Test
    fun `formats a whole number of hours without minutes`() {
        assertEquals("۲ ساعت", PersianFormat.formatDurationMinutes(120))
    }

    @Test
    fun `formats a sub hour duration in minutes only`() {
        assertEquals("۴۵ دقیقه", PersianFormat.formatDurationMinutes(45))
    }

    @Test
    fun `returns a placeholder for an unknown duration`() {
        assertEquals("—", PersianFormat.formatDurationMinutes(0))
    }

    @Test
    fun `formats a playback position under an hour`() {
        assertEquals("۰۲:۰۵", PersianFormat.formatPlaybackPosition(125_000L))
    }

    @Test
    fun `formats a playback position over an hour`() {
        assertEquals("۱:۰۲:۰۵", PersianFormat.formatPlaybackPosition(3_725_000L))
    }

    @Test
    fun `formats a zero playback position`() {
        assertEquals("۰۰:۰۰", PersianFormat.formatPlaybackPosition(0L))
    }
}
