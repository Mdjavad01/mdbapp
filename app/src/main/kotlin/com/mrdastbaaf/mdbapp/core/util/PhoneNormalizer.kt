package com.mrdastbaaf.mdbapp.core.util

/**
 * Phone-number normalisation, mirroring what the bridge does server-side.
 *
 * The plugin accepts a leading `+98` or `0098` and strips it, then requires
 * `^09[0-9]{9}$` — ASCII only. It does **not** convert Persian digits on the
 * inbound path (its `phone_variants()` helper handles Persian digits only for
 * *lookups* of already-stored numbers). So the client is responsible for both
 * conversions, and getting either wrong produces the same unhelpful result: a
 * `400 invalid_phone`.
 *
 * Rule: normalise on every keystroke-free boundary (submit, not typing) and keep
 * the field itself showing what the user typed.
 */
object PhoneNormalizer {

    private val IRANIAN_MOBILE = Regex("^09[0-9]{9}$")

    /**
     * Converts user input into the canonical `09XXXXXXXXX` form the API expects.
     *
     * Handles: Persian/Arabic digits, `+98`, `0098`, `98`, spaces, dashes,
     * parentheses, and a Persian keyboard's ZWNJ. Returns `null` when the result
     * still is not a valid Iranian mobile number, so the caller can show a
     * validation error rather than sending a request that will fail.
     */
    fun normalize(raw: String): String? {
        val ascii = PersianFormat.toAsciiDigits(raw)

        val stripped = ascii
            .filter { it.isDigit() }
            .takeIf { it.isNotEmpty() }
            ?: return null

        val national = when {
            stripped.startsWith("0098") -> "0" + stripped.removePrefix("0098")
            stripped.startsWith("98") && stripped.length >= 12 -> "0" + stripped.removePrefix("98")
            stripped.startsWith("9") && stripped.length == 10 -> "0$stripped"
            else -> stripped
        }

        return national.takeIf { IRANIAN_MOBILE.matches(it) }
    }

    /** True when [raw] is already canonical, so the UI can enable the submit button. */
    fun isValid(raw: String): Boolean = normalize(raw) != null

    /**
     * Masks a number for display: `0912***6789`.
     *
     * Matches the format the server uses for `maskedPhone` and `watermarkText`,
     * so a locally-rendered mask and a server-provided one look identical.
     * Prefer the server's value when one is available — this exists only for
     * pre-login screens where the server has not spoken yet.
     */
    fun mask(phone: String): String {
        val normalized = normalize(phone) ?: return phone
        return normalized.take(4) + "***" + normalized.takeLast(4)
    }
}
