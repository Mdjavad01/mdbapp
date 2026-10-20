package com.mrdastbaaf.mdbapp.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

/**
 * A kilim lattice, drawn rather than photographed.
 *
 * A carpet photo behind a headline fights the text: it is busy where the text
 * needs quiet, and it is heavy in the APK. This is the same idea reduced to
 * what a handwoven geometric border actually is — a repeating lozenge grid —
 * drawn at low opacity so it reads as texture, not as content.
 *
 * Static: drawn once per size change, never animated, so it costs nothing while
 * the user scrolls.
 */
@Composable
fun KilimPattern(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    alpha: Float = 0.10f,
) {
    Canvas(modifier = modifier) {
        val cell = 54.dp.toPx()
        val stroke = Stroke(width = 1.dp.toPx())
        val lineColor = color.copy(alpha = alpha)
        val dotColor = color.copy(alpha = alpha * 0.7f)

        var row = 0
        var y = 0f
        while (y < size.height + cell) {
            // Offset every other row so the lozenges interlock like a weave
            // instead of stacking in a plain grid.
            val shift = if (row % 2 == 0) 0f else cell / 2f
            var x = -cell + shift
            while (x < size.width + cell) {
                val diamond = Path().apply {
                    moveTo(x, y - cell / 2f)
                    lineTo(x + cell / 2f, y)
                    lineTo(x, y + cell / 2f)
                    lineTo(x - cell / 2f, y)
                    close()
                }
                drawPath(diamond, color = lineColor, style = stroke)

                // A single filled dot at each crossing gives the lattice a
                // woven rhythm without turning it into a polka-dot pattern.
                drawCircle(
                    color = dotColor,
                    radius = 1.4.dp.toPx(),
                    center = Offset(x, y),
                )

                x += cell
            }
            y += cell / 2f
            row++
        }
    }
}
