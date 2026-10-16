package com.mrdastbaaf.mdbapp.core.constants

/**
 * Content-security tuning.
 *
 * Kept as named constants rather than magic numbers so the values are reviewable
 * in one place and can be discussed with the product owner without a code hunt.
 */
object SecurityConfig {

    /**
     * A playback ticket is valid for 15 minutes server-side. The client does not
     * cache or persist it: a fresh ticket is requested immediately before every
     * playback, and the ticket plus the resulting video URL live in memory only.
     */
    const val TICKET_LIFETIME_SECONDS: Long = 15 * 60

    /**
     * Refresh the ticket when the remaining lifetime drops below this. Only
     * relevant once HLS lands — with the current direct-MP4 flow the URL keeps
     * playing after the ticket expires, so nothing interrupts playback.
     */
    const val TICKET_REFRESH_THRESHOLD_SECONDS: Long = 120

    /**
     * Refuse to play a `proxyMode: true` response.
     *
     * The PHP stream proxy issues a 60-second token that is NOT bound to the
     * requesting user, so a long video dies mid-playback and the URL is
     * shareable within that minute. Until the server issues a device-bound,
     * renewable session token, the app treats proxy mode as a hard error
     * instead of silently producing a broken player.
     */
    const val REJECT_PROXY_MODE: Boolean = true

    /** Root/emulator checks are cached for this long to avoid repeated syscalls. */
    const val DEVICE_INTEGRITY_CACHE_MILLIS: Long = 60_000L
}
