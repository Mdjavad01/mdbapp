package com.mrdastbaaf.mdbapp.core.constants

/**
 * Dynamic watermark tuning for the player overlay.
 *
 * The watermark text and user id always come from the server (`watermarkText` /
 * `watermarkUserId` on the playback ticket, plus `fullName` from `/auth/me`).
 * Nothing here derives identity locally — these are presentation constants only.
 */
object WatermarkConfig {

    /** 0f..1f. High enough to survive a phone-camera recording, low enough not to ruin the video. */
    const val OPACITY: Float = 0.35f

    /** Text size in sp at the default font scale. */
    const val TEXT_SIZE_SP: Float = 14f

    /** How often the watermark jumps to a new random position. */
    const val MOVE_INTERVAL_MILLIS: Long = 7_000L

    /** Animate the jump so it is visible rather than a jarring cut. */
    const val MOVE_ANIMATION_MILLIS: Int = 400

    /** Fade the watermark in/out so it never fully disappears from a recording. */
    const val FADE_DURATION_MILLIS: Int = 250

    /**
     * Anchor positions, expressed as a fraction of the *available* space
     * (0f..1f on each axis) rather than absolute offsets, so the watermark lands
     * sensibly on phones, tablets and in fullscreen alike.
     *
     * Six anchors is deliberate: enough spread that a crop cannot remove the
     * mark, few enough that the movement reads as intentional.
     */
    val POSITIONS: List<Position> = listOf(
        Position(0.04f, 0.06f), // top-start
        Position(0.62f, 0.06f), // top-end
        Position(0.04f, 0.50f), // middle-start
        Position(0.62f, 0.50f), // middle-end
        Position(0.04f, 0.88f), // bottom-start
        Position(0.62f, 0.88f), // bottom-end
    )

    /** Never repeat the same anchor twice in a row. */
    const val AVOID_IMMEDIATE_REPEAT: Boolean = true

    data class Position(val xFraction: Float, val yFraction: Float)
}
