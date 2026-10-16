package com.mrdastbaaf.mdbapp.core.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

/**
 * Certificate pinning, behind the `ENABLE_CERT_PINNING` build flag.
 *
 * Implemented with OkHttp's [CertificatePinner] rather than a custom trust
 * manager: a pinner only *narrows* what the platform trust store already
 * accepts, so a mistake can make the app stricter than intended but never
 * laxer. A `badCertificateCallback`-style override risks exactly the opposite.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE ENABLING — read this:
 *
 *  1. Pin the SPKI hash of the **intermediate CA**, not the leaf certificate.
 *     The host sits behind BitNinja/Cloudflare, so the leaf rotates frequently
 *     and a leaf pin will brick the app on the next rotation.
 *  2. Always ship at least two pins: the current key and the next one. A single
 *     pin with no backup means a rotation takes the app offline until every user
 *     updates.
 *  3. Set the `<pin-set expiration>` date in network_security_config.xml and
 *     rotate before it lapses.
 *  4. Test on a real device on a real network before release. Pinning failures
 *     are invisible on an emulator behind a debugging proxy.
 *  5. Never pin `dll.mrdastbaaf.com` to the same values as the API host — they
 *     are different certificates on different infrastructure.
 * ─────────────────────────────────────────────────────────────────────────────
 */
object PinningConfig {

    /**
     * Placeholder pins.
     *
     * These are intentionally not real. Enabling the flag without replacing them
     * makes the app fail loudly on the first request instead of appearing to pin
     * something. Replace both entries per host before switching the flag on.
     */
    private val PINS: Map<String, List<String>> = mapOf(
        "mrdastbaaf.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // TODO: intermediate CA SPKI
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=", // TODO: backup SPKI
        ),
        "dll.mrdastbaaf.com" to listOf(
            "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=", // TODO: intermediate CA SPKI
            "sha256/DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD=", // TODO: backup SPKI
        ),
    )

    /** Builds the pinner. Called only when `ENABLE_CERT_PINNING` is true. */
    fun build(): CertificatePinner = CertificatePinner.Builder().apply {
        PINS.forEach { (host, pins) ->
            pins.forEach { pin -> add(host, pin) }
        }
    }.build()

    /** Applies pinning to the client under construction. */
    fun apply(builder: OkHttpClient.Builder) {
        builder.certificatePinner(build())
    }

    /** Exposed so a unit test can assert that every host has a backup pin. */
    internal fun hosts(): Set<String> = PINS.keys

    internal fun pinCountFor(host: String): Int = PINS[host]?.size ?: 0
}
