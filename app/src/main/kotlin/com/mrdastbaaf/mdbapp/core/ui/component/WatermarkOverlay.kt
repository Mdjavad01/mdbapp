package com.mrdastbaaf.mdbapp.core.ui.component

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * A tiled, diagonal watermark over the video.
 *
 * The point is not decoration. A single label in a corner can be cropped out of
 * a screen recording; a repeating diagonal pattern covers the frame, so any
 * capture carries the buyer's identity. It drifts every twelve seconds for the
 * same reason — a still overlay can be masked with a static patch, a moving one
 * cannot.
 *
 * This is deterrence and attribution, not protection: a rooted device can still
 * capture the decoded frames. The only real fix is server-side signed segment
 * URLs, which is what the `videoType`/`expiresAt` fields are for.
 */
@Composable
fun WatermarkOverlay(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return

    val density = LocalDensity.current
    val stepHeight = 96.dp

    // Step rather than animate continuously: a 1.4-second move every twelve
    // seconds costs a fraction of the frames of a permanent animation, which
    // matters when the same surface is decoding video.
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        while (true) {
            delay(DRIFT_INTERVAL_MILLIS)
            step++
        }
    }

    val offset by animateFloatAsState(
        targetValue = (step % 3) * stepHeight.value,
        animationSpec = tween(durationMillis = 1_400),
        label = "watermark-drift",
    )

    val paint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            alpha = 34
            textSize = with(density) { 12.sp.toPx() }
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    Canvas(modifier = modifier) {
        val stepX = with(density) { 220.dp.toPx() }
        val stepY = with(density) { stepHeight.toPx() }
        val startY = -size.height + with(density) { offset.dp.toPx() }

        rotate(degrees = -24f) {
            var y = startY
            while (y < size.height * 2f) {
                var x = -size.width
                while (x < size.width * 2f) {
                    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
                    x += stepX
                }
                y += stepY
            }
        }
    }
}

private const val DRIFT_INTERVAL_MILLIS = 12_000L