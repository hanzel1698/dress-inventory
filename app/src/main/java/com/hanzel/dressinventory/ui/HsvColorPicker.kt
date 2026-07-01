package com.hanzel.dressinventory.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hanzel.dressinventory.data.COLOR_CHART
import com.hanzel.dressinventory.data.ChartColor
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

// ── Conversion helpers ────────────────────────────────────────────────────────

fun hsvToRgbColor(h: Float, s: Float, v: Float): Color {
    val hsv = floatArrayOf(h, s, v)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

/** Returns the hex as a 0xFF______L long (fully opaque). */
fun colorToHex(color: Color): Long =
    0xFF000000L or (color.toArgb().toLong() and 0x00FFFFFFL)

/** Nearest name from the curated chart by Euclidean RGB distance. */
fun closestChartColor(hex: Long): ChartColor {
    val r1 = (hex shr 16) and 0xFF
    val g1 = (hex shr 8) and 0xFF
    val b1 = hex and 0xFF
    return COLOR_CHART.minByOrNull { c ->
        val r2 = (c.hex shr 16) and 0xFF
        val g2 = (c.hex shr 8) and 0xFF
        val b2 = c.hex and 0xFF
        val dr = r1 - r2; val dg = g1 - g2; val db = b1 - b2
        dr * dr + dg * dg + db * db
    } ?: COLOR_CHART.first()
}

// ── Main composable ───────────────────────────────────────────────────────────

/**
 * HSV colour picker matching the design:
 *  - Outer rainbow ring  → selects Hue
 *  - Inner square        → selects Saturation (x) and Value/brightness (y)
 *
 * [onColorChanged] is invoked with (hexLong, colourName) whenever either
 * the hue, saturation, or value changes.
 */
@Composable
fun HsvColorPicker(
    initialHex: Long,
    onColorChanged: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initColor = Color(initialHex)
    val (initH, initS, initV) = remember(initialHex) { colorToHsv(initColor) }

    var hue by remember { mutableFloatStateOf(initH) }
    var sat by remember { mutableFloatStateOf(initS) }
    var value by remember { mutableFloatStateOf(initV) }

    val density = LocalDensity.current

    // We use BoxWithConstraints just to get a stable pixel size for the hit-test
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val sizePx = with(density) { maxWidth.toPx() }
        val ringFrac = 0.15f              // ring thickness as fraction of radius
        val outerR = sizePx / 2f
        val innerR = outerR * (1f - ringFrac * 2)   // inner edge of ring
        val squareHalf = innerR / sqrt(2f)           // largest square that fits the circle

        fun notifyChange(h: Float, s: Float, v: Float) {
            val c = hsvToRgbColor(h, s, v)
            val hex = colorToHex(c)
            onColorChanged(hex, closestChartColor(hex).name)
        }

        fun handleRingTouch(offset: Offset) {
            val dx = offset.x - outerR
            val dy = offset.y - outerR
            val dist = hypot(dx, dy)
            if (dist in (innerR - 4f)..(outerR + 4f)) {
                val angle = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f
                hue = angle
                notifyChange(hue, sat, value)
            }
        }

        fun handleSquareTouch(offset: Offset) {
            val cx = outerR; val cy = outerR
            val left = cx - squareHalf; val top = cy - squareHalf
            val right = cx + squareHalf; val bottom = cy + squareHalf
            if (offset.x in (left - 4f)..(right + 4f) && offset.y in (top - 4f)..(bottom + 4f)) {
                sat = ((offset.x - left) / (squareHalf * 2)).coerceIn(0f, 1f)
                value = (1f - (offset.y - top) / (squareHalf * 2)).coerceIn(0f, 1f)
                notifyChange(hue, sat, value)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(sizePx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val dx = offset.x - outerR; val dy = offset.y - outerR
                            val dist = hypot(dx, dy)
                            if (dist >= innerR - 4f) handleRingTouch(offset)
                            else handleSquareTouch(offset)
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            val dx = offset.x - outerR; val dy = offset.y - outerR
                            val dist = hypot(dx, dy)
                            if (dist >= innerR - 4f) handleRingTouch(offset)
                            else handleSquareTouch(offset)
                        }
                    )
                }
                .pointerInput(sizePx) {
                    detectTapGestures { offset ->
                        val dx = offset.x - outerR; val dy = offset.y - outerR
                        val dist = hypot(dx, dy)
                        if (dist >= innerR - 4f) handleRingTouch(offset)
                        else handleSquareTouch(offset)
                    }
                }
        ) {
            val center = Offset(outerR, outerR)

            // ── 1. Hue ring ──────────────────────────────────────────────────
            drawHueRing(center, outerR, innerR)

            // Hue ring cursor
            val hueRad = (hue * PI / 180).toFloat()
            val cursorR = (outerR + innerR) / 2f
            val cursorPos = Offset(
                center.x + cursorR * cos(hueRad),
                center.y + cursorR * sin(hueRad),
            )
            drawCircle(Color.White, radius = (outerR - innerR) / 2f - 2.dp.toPx(), center = cursorPos,
                style = Stroke(width = 3.dp.toPx()))
            drawCircle(hsvToRgbColor(hue, 1f, 1f), radius = (outerR - innerR) / 2f - 4.dp.toPx(), center = cursorPos)

            // ── 2. SV square ─────────────────────────────────────────────────
            val squareSize = squareHalf * 2
            val squareTopLeft = Offset(center.x - squareHalf, center.y - squareHalf)

            drawSvSquare(squareTopLeft, squareSize, hue)

            // Square border
            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = squareTopLeft,
                size = Size(squareSize, squareSize),
                style = Stroke(width = 1.dp.toPx()),
            )

            // SV cursor
            val svX = squareTopLeft.x + sat * squareSize
            val svY = squareTopLeft.y + (1f - value) * squareSize
            val svCursor = Offset(svX, svY)
            drawCircle(Color.White, radius = 8.dp.toPx(), center = svCursor,
                style = Stroke(width = 2.5.dp.toPx()))
            drawCircle(Color.Black, radius = 5.dp.toPx(), center = svCursor,
                style = Stroke(width = 1.dp.toPx()))
            drawCircle(hsvToRgbColor(hue, sat, value), radius = 5.dp.toPx(), center = svCursor)
        }
    }
}

// ── Drawing helpers ───────────────────────────────────────────────────────────

/**
 * Draws a smooth hue ring by rendering 360 thin arc segments, each filled with
 * the hue colour at that angle.  The ring is anti-aliased and seamless.
 */
private fun DrawScope.drawHueRing(center: Offset, outerR: Float, innerR: Float) {
    val strokeW = outerR - innerR
    val trackR = (outerR + innerR) / 2f

    // 1. Draw the full sweep gradient first (smooth rainbow ring)
    val hueColors = listOf(
        Color.Red, Color(0xFFFF7F00), Color.Yellow, Color(0xFF7FFF00),
        Color.Green, Color(0xFF00FF7F), Color.Cyan, Color(0xFF007FFF),
        Color.Blue, Color(0xFF7F00FF), Color.Magenta, Color(0xFFFF007F),
        Color.Red,
    )
    drawCircle(
        brush = Brush.sweepGradient(hueColors, center = center),
        radius = trackR,
        center = center,
        style = Stroke(width = strokeW),
    )
}

/**
 * Draws the SV (Saturation × Value) square for a given hue.
 * - Left→Right: white (S=0) → full hue (S=1)
 * - Top→Bottom: full brightness (V=1) → black (V=0)
 */
private fun DrawScope.drawSvSquare(topLeft: Offset, size: Float, hue: Float) {
    val pureHue = hsvToRgbColor(hue, 1f, 1f)

    // Horizontal saturation gradient (white → hue colour)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White, pureHue),
            startX = topLeft.x,
            endX = topLeft.x + size,
        ),
        topLeft = topLeft,
        size = Size(size, size),
    )

    // Vertical value gradient (transparent → black), blended on top
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startY = topLeft.y,
            endY = topLeft.y + size,
        ),
        topLeft = topLeft,
        size = Size(size, size),
    )
}
