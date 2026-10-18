package com.mrdastbaaf.mdbapp.features.player.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.core.network.safeApiCall
import com.mrdastbaaf.mdbapp.features.player.data.remote.dto.LessonProgressRequest
import com.mrdastbaaf.mdbapp.features.player.data.remote.dto.PlaybackDto
import com.mrdastbaaf.mdbapp.features.player.data.remote.dto.PlaybackRequestDto
import com.mrdastbaaf.mdbapp.features.player.domain.model.LessonProgress
import com.mrdastbaaf.mdbapp.features.player.domain.model.PlaybackQuality
import com.mrdastbaaf.mdbapp.features.player.domain.model.PlaybackSession
import com.mrdastbaaf.mdbapp.features.player.domain.repository.PlayerRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The two-step playback handshake.
 *
 * **Step 1 is the access decision.** `POST /lessons/{id}/playback-ticket` answers
 * 403 with a Persian explanation when the user may not watch, and that text is
 * shown verbatim — the app never decides locally whether a lesson is playable.
 *
 * **`streamType` is not trusted.** The current server hard-codes `"hls"` while
 * serving MP4 files. HLS is detected from the URL itself (`.m3u8`, ignoring the
 * query string), and from `videoType` only where the v1.8.0 patch has been
 * deployed. Guessing from the extension is the one thing the review said would
 * break with proxy URLs, so the fallback is "progressive", which every browser
 * and ExoPlayer can play.
 */
@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val api: MdbApi,
    private val json: Json,
) : PlayerRepository {

    override suspend fun openLesson(lessonId: String): Result<PlaybackSession> {
        val ticket = safeApiCall(json) { api.requestPlaybackTicket(lessonId) }
            .getOrElse { return Result.failure(it) }

        val ticketId = ticket.ticketId?.takeIf { it.isNotBlank() }
            ?: return Result.failure(
                Failure.InvalidResponse("بلیت پخش از سرور دریافت نشد."),
            )

        return safeApiCall(json) {
            api.resolvePlayback(lessonId, PlaybackRequestDto(ticketId = ticketId))
        }.mapCatching { dto ->
            dto.toDomain(ticket.watermarkText) ?: throw Failure.InvalidResponse(
                "نشانی ویدیو از سرور دریافت نشد.",
            )
        }
    }

    override suspend fun progress(lessonId: String): Result<LessonProgress?> {
        val result = safeApiCall(json) { api.getLessonProgress(lessonId) }

        return result.fold(
            onSuccess = { dto ->
                Result.success<LessonProgress?>(
                    LessonProgress(
                        positionSeconds = dto.positionSeconds?.coerceAtLeast(0) ?: 0,
                        durationSeconds = dto.durationSeconds?.coerceAtLeast(0) ?: 0,
                        completed = dto.completed == true,
                    ),
                )
            },
            onFailure = { throwable ->
                // 404 means the server predates the progress endpoint. That is
                // "no resume point", not a failure worth showing anyone.
                if ((throwable as? Failure) is Failure.NotFound) {
                    Result.success<LessonProgress?>(null)
                } else {
                    Result.failure(throwable)
                }
            },
        )
    }

    override suspend fun saveProgress(
        lessonId: String,
        positionSeconds: Int,
        durationSeconds: Int,
    ): Result<Unit> = safeApiCall(json) {
        api.saveLessonProgress(
            lessonId,
            LessonProgressRequest(
                positionSeconds = positionSeconds,
                durationSeconds = durationSeconds.takeIf { it > 0 },
            ),
        )
    }.map { }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private fun PlaybackDto.toDomain(ticketWatermark: String?): PlaybackSession? {
        // Refused by agreement: the proxy token lives in a 60-second transient,
        // so a proxy stream dies about a minute in. Better a clear error than a
        // video that stops for no visible reason.
        if (proxyMode == true) return null

        val url = videoUrl?.takeIf { it.isNotBlank() } ?: return null

        val qualities = qualities.mapNotNull { quality ->
            val qualityUrl = quality.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PlaybackQuality(
                label = quality.label?.takeIf { it.isNotBlank() } ?: "خودکار",
                url = qualityUrl,
                isHls = isHls(qualityUrl, quality.type),
            )
        }

        return PlaybackSession(
            videoUrl = url,
            watermarkText = watermarkText?.takeIf { it.isNotBlank() }
                ?: ticketWatermark.orEmpty(),
            qualities = qualities,
            isHls = isHls(url, videoType),
        )
    }

    /**
     * `declaredType` is `videoType` only — never `streamType`, which the current
     * server hard-codes to `"hls"` regardless of what it serves.
     */
    private fun isHls(url: String, declaredType: String?): Boolean {
        if (declaredType?.equals("hls", ignoreCase = true) == true) return true
        if (declaredType?.equals("mp4", ignoreCase = true) == true) return false

        return url.substringBefore('?').substringBefore('#')
            .endsWith(".m3u8", ignoreCase = true)
    }
}
