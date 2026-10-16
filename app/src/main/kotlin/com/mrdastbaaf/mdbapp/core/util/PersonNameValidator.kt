package com.mrdastbaaf.mdbapp.core.util

/**
 * Validates a Persian given name or surname.
 *
 * This mirrors `sanitize_person_name()` in `class-auth-controller.php` **exactly**,
 * and that is the whole point of the class. The server keeps only characters in
 * the Arabic/Persian Unicode block plus the zero-width non-joiner and spaces,
 * then collapses runs of whitespace and truncates to 60 characters. Anything
 * else — including every Latin letter — is **silently discarded**.
 *
 * Silent discarding is the dangerous part. A user who types `Mohammad` would get
 * an account named `هنرجو 1234` with no indication anything went wrong. So the
 * client validates with the same rule and *tells* the user, instead of relying on
 * a server that will happily accept the request and throw the value away.
 *
 * Rule: only Persian/Arabic letters (U+0600–U+06FF), ZWNJ (U+200C) and spaces.
 */
object PersonNameValidator {

    /** Same ceiling as the server. */
    const val MAX_LENGTH = 60

    private const val ARABIC_BLOCK_START = '\u0600'
    private const val ARABIC_BLOCK_END = '\u06FF'

    /** U+200C — the zero-width non-joiner, as in «نیم‌فاصله». */
    private const val ZWNJ = '\u200C'

    private val WHITESPACE_RUN = Regex("\\s+")

    /** True for a character the server's `preg_replace` would keep. */
    private fun Char.isAllowed(): Boolean =
        this in ARABIC_BLOCK_START..ARABIC_BLOCK_END || this == ZWNJ || this == ' '

    /**
     * Strips everything the server would strip.
     *
     * Used to normalise before sending so that what the user sees in the field is
     * what ends up on the account.
     */
    fun sanitize(raw: String): String = raw
        .filter { it.isAllowed() }
        .replace(WHITESPACE_RUN, " ")
        .trim()
        .take(MAX_LENGTH)

    /**
     * True when [raw] contains only characters the server will keep.
     *
     * An empty string is considered valid: the server treats both name parts as
     * optional, and for an existing account they are not sent at all.
     */
    fun isAcceptable(raw: String): Boolean = raw.all { it.isAllowed() }

    /**
     * The first offending character, for a message that names the problem rather
     * than saying "invalid". Returns `null` when there is nothing to report.
     */
    fun firstInvalidCharacter(raw: String): Char? = raw.firstOrNull { !it.isAllowed() }

    /** True when the name is too long to survive the server's truncation. */
    fun isTooLong(raw: String): Boolean = raw.length > MAX_LENGTH
}
