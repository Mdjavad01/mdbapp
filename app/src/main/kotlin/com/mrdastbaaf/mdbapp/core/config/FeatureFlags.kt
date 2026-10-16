package com.mrdastbaaf.mdbapp.core.config

/**
 * Compile-time feature flags.
 *
 * These are constants, not remote config: a flag that could be flipped by a
 * server response would be a security hole for the ones that matter (root
 * blocking, watermarking, pinning). When `GET /app-config` lands, only the
 * *cosmetic* flags should become remote.
 */
object FeatureFlags {

    /** Overlay the moving identity watermark on the player. Never disable in release. */
    const val WATERMARK_ENABLED: Boolean = true

    /** Block playback on rooted devices and emulators. */
    const val BLOCK_ROOTED_DEVICES: Boolean = true

    /** Apply FLAG_SECURE to the player and paid-content screens. */
    const val SECURE_SCREEN_ENABLED: Boolean = true

    /** Pause playback when the app is backgrounded, re-validate on return. */
    const val PAUSE_ON_BACKGROUND: Boolean = true

    /** Physical-products showcase (read-only, opens the website to buy). */
    const val SHOP_SHOWCASE_ENABLED: Boolean = true

    /** Resume from the last position. Requires the server endpoint; off until it ships. */
    const val RESUME_PLAYBACK_ENABLED: Boolean = false
}
