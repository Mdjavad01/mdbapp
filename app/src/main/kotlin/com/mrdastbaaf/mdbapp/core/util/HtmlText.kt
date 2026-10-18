package com.mrdastbaaf.mdbapp.core.util

/**
 * Turns the bridge's HTML fragments into readable plain text.
 *
 * `description` is `post_excerpt`, and the client that fills it in the WordPress
 * editor stores HTML — `<p style="text-align: right">…</p>`. Rendering that
 * verbatim in a `Text` shows the markup to the user, which is what the first
 * build did.
 *
 * A `Html.fromHtml` call would be the obvious answer, but it is an Android API
 * and these mappers run in plain JVM unit tests, where it returns null. A small
 * pure-Kotlin stripper keeps the mapping testable and has no dependency on the
 * platform.
 *
 * It is deliberately not a general-purpose sanitiser: the input is our own
 * excerpt, never user-submitted HTML that would be rendered back.
 */
object HtmlText {

    /** Block-level tags become line breaks; everything else just disappears. */
    private val BLOCK_BREAK = Regex(
        "(?i)<br\\s*/?>|</p\\s*>|</div\\s*>|</li\\s*>|</h[1-6]\\s*>|</tr\\s*>",
    )

    private val TAG = Regex("<[^>]*>")

    private val HORIZONTAL_SPACE = Regex("[\\t\\u000B\\f\\r ]+")

    private val EXCESS_NEWLINES = Regex("\\n{3,}")

    private val NUMERIC_ENTITY = Regex("&#(?:[xX]([0-9a-fA-F]+)|([0-9]+));")

    /** Strips tags and decodes the entities WordPress actually emits. */
    fun strip(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var text = BLOCK_BREAK.replace(input, "\n")
        text = TAG.replace(text, "")
        text = decodeEntities(text)
        text = HORIZONTAL_SPACE.replace(text, " ")

        // Trim each line, drop leading/trailing blanks, and never leave more
        // than one empty line between paragraphs.
        text = text.lineSequence()
            .map { it.trim() }
            .joinToString("\n")
            .trim()

        return EXCESS_NEWLINES.replace(text, "\n\n")
    }

    private fun decodeEntities(input: String): String {
        var text = input
            .replace("&nbsp;", " ")
            .replace("&zwnj;", "\u200C")
            .replace("&hellip;", "…")
            .replace("&raquo;", "»")
            .replace("&laquo;", "«")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

        text = NUMERIC_ENTITY.replace(text) { match ->
            val hex = match.groupValues[1]
            val dec = match.groupValues[2]
            val codePoint = if (hex.isNotEmpty()) hex.toIntOrNull(16) else dec.toIntOrNull()
            codePoint?.takeIf { Character.isValidCodePoint(it) }
                ?.let { String(Character.toChars(it)) }
                ?: match.value
        }

        // Last, so that `&amp;lt;` becomes `&lt;` rather than `<`.
        return text.replace("&amp;", "&")
    }
}
