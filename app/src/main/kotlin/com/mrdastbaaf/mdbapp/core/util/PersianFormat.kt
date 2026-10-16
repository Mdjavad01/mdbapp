package com.mrdastbaaf.mdbapp.core.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Persian digit and number formatting.
 *
 * The API speaks ASCII: every id, price and duration arrives as `0-9`, and every
 * request the app sends must use ASCII too — the bridge validates phone numbers
 * against `^09[0-9]{9}$`, which Persian-Indic digits fail. So the rule is
 * absolute and one-directional:
 *
 *   **ASCII on the wire, Persian on the screen.**
 *
 * [toPersianDigits] is therefore only ever applied at the presentation boundary,
 * never to a value that is about to be sent back to the server.
 */
object PersianFormat {

    private const val PERSIAN_ZERO = '\u06F0'   // ۰
    private const val PERSIAN_NINE = '\u06F9'   // ۹
    private const val ARABIC_ZERO = '\u0660'    // ٠ — used by Arabic-locale keyboards
    private const val ARABIC_NINE = '\u0669'    // ٩
    private const val THOUSANDS_SEPARATOR = '\u066C' // ٬

    private val faLocale = Locale.forLanguageTag("fa-IR")

    /**
     * Converts ASCII digits to Persian-Indic digits.
     *
     * Only `0-9` are touched; everything else (spaces, currency words, the
     * thousands separator) passes through unchanged.
     */
    fun toPersianDigits(input: String): String = buildString(input.length) {
        input.forEach { char ->
            append(
                when (char) {
                    in '0'..'9' -> PERSIAN_ZERO + (char - '0')
                    else -> char
                },
            )
        }
    }

    /**
     * Converts Persian-Indic and Arabic-Indic digits back to ASCII.
     *
     * Used on user input — a Persian keyboard produces `۰۹۱۲…`, and the server
     * rejects it. Every phone number and OTP code goes through here before it
     * leaves the app.
     */
    fun toAsciiDigits(input: String): String = buildString(input.length) {
        input.forEach { char ->
            append(
                when (char) {
                    in PERSIAN_ZERO..PERSIAN_NINE -> '0' + (char - PERSIAN_ZERO)
                    in ARABIC_ZERO..ARABIC_NINE -> '0' + (char - ARABIC_ZERO)
                    else -> char
                },
            )
        }
    }

    /**
     * Formats a Toman amount for display: `۴۹۰٬۰۰۰ تومان`.
     *
     * Uses the Persian locale's grouping so the separator is the Persian
     * thousands mark rather than a Latin comma.
     */
    fun formatToman(amount: Long, withUnit: Boolean = true): String {
        val grouped = NumberFormat.getIntegerInstance(faLocale).format(amount)
        val digits = toPersianDigits(grouped)
        return if (withUnit) "$digits تومان" else digits
    }

    /** Plain grouped number, Persian digits, no unit. For counts and ids. */
    fun formatNumber(value: Long): String =
        toPersianDigits(NumberFormat.getIntegerInstance(faLocale).format(value))

    /**
     * Formats a lesson/course duration in minutes as Persian prose:
     * `۲ ساعت و ۳۰ دقیقه`, `۴۵ دقیقه`, `۲ ساعت`.
     */
    fun formatDurationMinutes(minutes: Int): String {
        if (minutes <= 0) return "—"
        val hours = minutes / 60
        val remaining = minutes % 60
        return when {
            hours > 0 && remaining > 0 ->
                "${formatNumber(hours.toLong())} ساعت و ${formatNumber(remaining.toLong())} دقیقه"

            hours > 0 -> "${formatNumber(hours.toLong())} ساعت"
            else -> "${formatNumber(remaining.toLong())} دقیقه"
        }
    }

    /**
     * Formats a playback position as `mm:ss` / `h:mm:ss` with Persian digits.
     * Player chrome only — never sent anywhere.
     */
    fun formatPlaybackPosition(millis: Long): String {
        if (millis <= 0L) return toPersianDigits("00:00")
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val raw = if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
        return toPersianDigits(raw)
    }
}
