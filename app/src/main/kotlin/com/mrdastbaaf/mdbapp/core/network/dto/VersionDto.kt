package com.mrdastbaaf.mdbapp.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /version` — public, unauthenticated, no cache.
 *
 * The bridge returns snake_case here (unlike almost every other endpoint, which
 * is camelCase), so every field carries an explicit [SerialName]. All fields are
 * nullable: a field added or removed server-side must never crash the client.
 *
 * This is the cheapest way to prove the whole stack works — DNS, TLS, the WAF,
 * LiteSpeed, WordPress and the plugin — without touching a credential.
 */
@Serializable
data class VersionDto(
    @SerialName("pwa_version") val pwaVersion: String? = null,
    @SerialName("plugin_version") val pluginVersion: String? = null,
    @SerialName("db_version") val dbVersion: String? = null,
    @SerialName("server_time") val serverTime: String? = null,
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("content_hash") val contentHash: String? = null,
)
