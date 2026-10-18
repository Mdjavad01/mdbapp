package com.mrdastbaaf.mdbapp.features.player.domain.model

/**
 * Everything needed to start playback, resolved by the two-step handshake.
 *
 * [videoUrl] is a short-lived credential in spirit even when the server hands
 * out a permanent CDN URL — it must never be persisted or logged. It lives in
 * memory for the length of the session and is re-resolved on every open.
 */
data class PlaybackSession(
    val videoUrl: String,
    /** Masked phone and id — the watermark is client-rendered, not burned in. */
    val watermarkText: String,
    val qualities: List<PlaybackQuality>,
    /** Decided from the URL, not from the server's `streamType`, which lies. */
    val isHls: Boolean,
)

data class PlaybackQuality(
    val label: String,
    val url: String,
    val isHls: Boolean,
)

/** Where the user stopped. Absent on a server without the v1.8.0 patch. */
data class LessonProgress(
    val positionSeconds: Int,
    val durationSeconds: Int,
    val completed: Boolean,
) {
    /**
     * Resuming into the last few seconds just replays the credits, and a
     * position at the very end is a completed lesson rather than a resume point.
     */
    val resumePositionSeconds: Int
        get() = when {
            completed -> 0
            durationSeconds <= 0 -> positionSeconds
            positionSeconds >= durationSeconds - RESUME_TAIL_SECONDS -> 0
            else -> positionSeconds
        }

    companion object {
        const val RESUME_TAIL_SECONDS = 10
    }
}
