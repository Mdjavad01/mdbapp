package com.mrdastbaaf.mdbapp.features.player.domain.repository

import com.mrdastbaaf.mdbapp.features.player.domain.model.LessonProgress
import com.mrdastbaaf.mdbapp.features.player.domain.model.PlaybackSession

/**
 * Playback, as the app is allowed to use it.
 *
 * The ticket never leaves this layer: [openLesson] performs both steps of the
 * handshake, so no screen or ViewModel can hold a ticket, cache a video URL, or
 * accidentally skip the access check.
 */
interface PlayerRepository {

    /** Ticket + source in one call. Access is decided entirely by the server. */
    suspend fun openLesson(lessonId: String): Result<PlaybackSession>

    /**
     * Resume point, or `null` when the server does not have the progress
     * endpoint yet (it 404s on anything before the v1.8.0 patch).
     */
    suspend fun progress(lessonId: String): Result<LessonProgress?>

    /** Best effort: a failure here must never interrupt playback. */
    suspend fun saveProgress(
        lessonId: String,
        positionSeconds: Int,
        durationSeconds: Int,
    ): Result<Unit>
}
