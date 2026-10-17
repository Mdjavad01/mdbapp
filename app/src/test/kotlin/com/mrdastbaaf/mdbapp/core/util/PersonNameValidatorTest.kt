package com.mrdastbaaf.mdbapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The name rule has to match the server's exactly, because the server does not
 * reject a bad name — it silently strips the offending characters. A user who
 * types a Latin name would end up with an account called `هنرجو 1234` and no
 * explanation, which is precisely the bug this class exists to prevent.
 */
class PersonNameValidatorTest {

    @Test
    fun `keeps persian letters and spaces`() {
        assertEquals("محمد جواد", PersonNameValidator.sanitize("محمد جواد"))
    }

    @Test
    fun `keeps the zero width non joiner used in persian compounds`() {
        // U+200C — as in «نیمفاصله». Dropping it would visibly corrupt the name.
        assertEquals("نیم\u200Cفاصله", PersonNameValidator.sanitize("نیم\u200Cفاصله"))
    }

    @Test
    fun `strips latin letters the server would silently drop`() {
        assertEquals("", PersonNameValidator.sanitize("Mohammad"))
        // A mixed name keeps the Persian part — and the leading space is trimmed.
        assertEquals("رضایی", PersonNameValidator.sanitize("Ali رضایی"))
    }

    @Test
    fun `strips ascii digits but keeps the persian-indic ones the server keeps`() {
        assertEquals("", PersonNameValidator.sanitize("123"))
        // U+06F0–U+06F9 lie inside the block the server whitelists
        // (`preg_replace('/[^\x{0600}-\x{06FF}\x{200C}\s]+/u', ...)`), so the
        // server stores them. Stripping them here would make the field show
        // something the account does not contain — the exact divergence this
        // class exists to prevent.
        assertEquals("۰۹۱۲", PersonNameValidator.sanitize("۰۹۱۲"))
    }

    @Test
    fun `strips punctuation and emoji`() {
        assertEquals("", PersonNameValidator.sanitize("!!!"))
        assertEquals("", PersonNameValidator.sanitize("\uD83D\uDE00"))
        assertEquals("", PersonNameValidator.sanitize("<script>"))
    }

    @Test
    fun `collapses repeated whitespace like the server does`() {
        assertEquals("علی رضایی", PersonNameValidator.sanitize("علی    رضایی"))
        assertEquals("علی رضایی", PersonNameValidator.sanitize("  علی رضایی  "))
    }

    @Test
    fun `truncates to sixty characters like the server does`() {
        val long = "ا".repeat(80)
        assertEquals(60, PersonNameValidator.sanitize(long).length)
    }

    @Test
    fun `accepts a pure persian name`() {
        assertTrue(PersonNameValidator.isAcceptable("محمدجواد"))
        assertTrue(PersonNameValidator.isAcceptable("علی رضایی"))
    }

    @Test
    fun `treats empty as acceptable because both name parts are optional`() {
        assertTrue(PersonNameValidator.isAcceptable(""))
    }

    @Test
    fun `rejects latin input so the user can be told`() {
        assertFalse(PersonNameValidator.isAcceptable("Mohammad"))
        assertFalse(PersonNameValidator.isAcceptable("علی Ali"))
    }

    @Test
    fun `reports the first offending character for a useful message`() {
        assertEquals('M', PersonNameValidator.firstInvalidCharacter("Mohammad"))
        assertEquals('A', PersonNameValidator.firstInvalidCharacter("علی Ali"))
        assertNull(PersonNameValidator.firstInvalidCharacter("علی رضایی"))
    }

    @Test
    fun `detects names the server would truncate`() {
        assertFalse(PersonNameValidator.isTooLong("ا".repeat(60)))
        assertTrue(PersonNameValidator.isTooLong("ا".repeat(61)))
    }
}
