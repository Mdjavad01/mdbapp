package com.mrdastbaaf.mdbapp.features.player.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Step 1 of the playback handshake.
 *
 * Every field is nullable because the ticket is the *access decision*: when it
 * fails, the body is an error envelope instead, and a parse failure must not
 * mask the status code.
 */
@Serializable
data class PlaybackTicketDto(
    val ticketId: String? = null,
    val lessonId: String? = null,
    val expiresInSeconds: Int? = null,
    val issuedAt: String? = null,
    val expiresAt: String? = null,
    /** `0912***4567 | ID: 42` — masked phone and numeric id. */
    val watermarkText: String? = null,
    val watermarkUserId: String? = null,
    /** Hard-coded `"hls"` by the current server; do not trust it — see the mapper. */
    val streamType: String? = null,
    /** Added by the v1.8.0 patch; absent on older servers. */
    val watermarkFullName: String? = null,
)

/** Step 2 request body. */
@Serializable
data class PlaybackRequestDto(
    val ticketId: String,
)

@Serializable
data class QualityDto(
    val label: String? = null,
    val url: String? = null,
    /** Added by the v1.8.0 patch; absent on older servers. */
    val type: String? = null,
)

/**
 * Step 2 response: the actual media URLs.
 *
 * `videoUrl` is a **permanent CDN URL** until the signed-URL patch is deployed,
 * so it must never be persisted — it lives in memory for the session only.
 */
@Serializable
data class PlaybackDto(
    val videoUrl: String? = null,
    val watermarkText: String? = null,
    val expiresAt: String? = null,
    val qualities: List<QualityDto> = emptyList(),
    val proxyMode: Boolean? = null,
    /** Added by the v1.8.0 patch: `"mp4"` or `"hls"`. */
    val videoType: String? = null,
)

/** `GET/PUT /lessons/{id}/progress` — v1.8.0 only. */
@Serializable
data class LessonProgressDto(
    val positionSeconds: Int? = null,
    val durationSeconds: Int? = null,
    val completed: Boolean? = null,
)

@Serializable
data class LessonProgressRequest(
    val positionSeconds: Int,
    val durationSeconds: Int? = null,
)
