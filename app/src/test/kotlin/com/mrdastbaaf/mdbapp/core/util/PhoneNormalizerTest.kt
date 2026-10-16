package com.mrdastbaaf.mdbapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The phone number is the one value the client must transform before it reaches
 * the server, and the server's validator (`^09[0-9]{9}$`) is ASCII-only and
 * unforgiving. Every case below is a shape a real Persian keyboard or a
 * copy-paste from a contact list produces.
 */
class PhoneNormalizerTest {

    @Test
    fun `accepts a canonical iranian mobile`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("09123456789"))
    }

    @Test
    fun `converts persian indic digits`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("۰۹۱۲۳۴۵۶۷۸۹"))
    }

    @Test
    fun `converts arabic indic digits`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("٠٩١٢٣٤٥٦٧٨٩"))
    }

    @Test
    fun `strips the plus ninety eight country code`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("+989123456789"))
    }

    @Test
    fun `strips the double zero country code`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("00989123456789"))
    }

    @Test
    fun `strips a bare ninety eight country code`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("989123456789"))
    }

    @Test
    fun `adds the leading zero to a nine prefixed number`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("9123456789"))
    }

    @Test
    fun `ignores spaces dashes and parentheses`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("0912 345 6789"))
        assertEquals("09123456789", PhoneNormalizer.normalize("0912-345-6789"))
        assertEquals("09123456789", PhoneNormalizer.normalize("(0912) 345 6789"))
    }

    @Test
    fun `handles a mixed persian and ascii input`() {
        assertEquals("09123456789", PhoneNormalizer.normalize("۰۹12۳۴۵۶۷۸۹"))
    }

    @Test
    fun `rejects a landline prefix`() {
        assertNull(PhoneNormalizer.normalize("02123456789"))
    }

    @Test
    fun `rejects a number that is too short`() {
        assertNull(PhoneNormalizer.normalize("0912345678"))
    }

    @Test
    fun `rejects a number that is too long`() {
        assertNull(PhoneNormalizer.normalize("091234567890"))
    }

    @Test
    fun `rejects empty and non numeric input`() {
        assertNull(PhoneNormalizer.normalize(""))
        assertNull(PhoneNormalizer.normalize("   "))
        assertNull(PhoneNormalizer.normalize("سلام"))
    }

    @Test
    fun `isValid mirrors normalize`() {
        assertTrue(PhoneNormalizer.isValid("۰۹۱۲۳۴۵۶۷۸۹"))
        assertFalse(PhoneNormalizer.isValid("12345"))
    }

    @Test
    fun `mask matches the servers watermark format`() {
        // The bridge builds `substr(0,4) . '***' . substr(-4)`, so a locally
        // rendered mask must be byte-identical or the watermark would change
        // shape between screens.
        assertEquals("0912***6789", PhoneNormalizer.mask("09123456789"))
        assertEquals("0912***6789", PhoneNormalizer.mask("۰۹۱۲۳۴۵۶۷۸۹"))
    }
}
