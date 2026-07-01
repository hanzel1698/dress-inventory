package com.hanzel.dressinventory.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.hanzel.dressinventory.data.COLOR_CHART
import com.hanzel.dressinventory.data.ChartColor

// ─────────────────────────────────────────────────────────────────────────────
//  Grid layout constants
// ─────────────────────────────────────────────────────────────────────────────

private const val COLS = 16   // col 0 = grayscale | col 1 = browns | cols 2-15 = 14 hues
private const val ROWS = 9    // row 0 = bright/pastel → row 8 = deep/dark

// ─────────────────────────────────────────────────────────────────────────────
//  Pre-built palette data  (object-level — computed once, zero allocs at draw)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * All palette ARGB values and per-cell luminance are computed once at startup.
 * No objects are created inside the draw loop.
 */
private object PaletteData {

    // 14 hue stops spanning the full spectrum (columns 2-15)
    private val HUE_COLUMNS = floatArrayOf(
        0f,    // Red
        15f,   // Coral / Orange-Red
        30f,   // Orange
        48f,   // Amber / Gold
        60f,   // Yellow
        88f,   // Yellow-Green / Lime
        130f,  // Green
        162f,  // Emerald / Jade
        188f,  // Teal
        210f,  // Sky Blue / Cerulean
        240f,  // Royal Blue
        265f,  // Indigo / Violet
        295f,  // Purple
        325f,  // Pink / Rose / Magenta
    )

    // Saturation steps: row 0 (pastel) → row 8 (deeply saturated-dark)
    private val SAT = floatArrayOf(0.18f, 0.38f, 0.58f, 0.75f, 0.90f, 1.00f, 1.00f, 0.95f, 0.88f)

    // Value steps: row 0 (bright) → row 8 (dark)
    private val VAL = floatArrayOf(1.00f, 1.00f, 0.98f, 0.95f, 0.90f, 0.80f, 0.60f, 0.40f, 0.22f)

    // Brown column uses lower saturation at the same hue (H≈25°) to produce
    // cream → tan → caramel → chocolate tones rather than oranges.
    private val BROWN_SAT = floatArrayOf(0.20f, 0.32f, 0.44f, 0.55f, 0.65f, 0.74f, 0.80f, 0.85f, 0.87f)
    private val BROWN_VAL = floatArrayOf(0.93f, 0.85f, 0.76f, 0.67f, 0.57f, 0.47f, 0.36f, 0.25f, 0.15f)
    private const val BROWN_HUE = 25f

    /** ARGB pixel values for every cell — indexed as ARGB[row][col]. */
    val ARGB: Array<IntArray> = buildGrid()

    /** Perceived luminance in [0,1] for every cell — used for adaptive ring colour. */
    val LUMINANCE: Array<FloatArray> = buildLuminance()

    private fun hsv(h: Float, s: Float, v: Float): Int =
        android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))

    private fun buildGrid(): Array<IntArray> {
        val grid = Array(ROWS) { IntArray(COLS) }

        for (row in 0 until ROWS) {
            // ── Column 0: pure grayscale ramp (white → black) ───────────────
            val grayV = 1f - row.toFloat() / (ROWS - 1).toFloat()
            grid[row][0] = hsv(0f, 0f, grayV)

            // ── Column 1: warm browns / cream-tan-chocolate ──────────────────
            grid[row][1] = hsv(BROWN_HUE, BROWN_SAT[row], BROWN_VAL[row])

            // ── Columns 2-15: 14 hue groups across the spectrum ──────────────
            val s = SAT[row]
            val v = VAL[row]
            for (i in HUE_COLUMNS.indices) {
                grid[row][i + 2] = hsv(HUE_COLUMNS[i], s, v)
            }
        }

        return grid
    }

    private fun buildLuminance(): Array<FloatArray> =
        Array(ROWS) { row ->
            FloatArray(COLS) { col ->
                val a = ARGB[row][col]
                val r = ((a shr 16) and 0xFF) / 255f
                val g = ((a shr 8) and 0xFF) / 255f
                val b = (a and 0xFF) / 255f
                // Rec. 601 perceived luminance
                0.299f * r + 0.587f * g + 0.114f * b
            }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared conversion helpers (referenced from other files in the package)
// ─────────────────────────────────────────────────────────────────────────────

fun hsvToRgbColor(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

/** Returns the colour as a fully-opaque 0xFF______L long. */
fun colorToHex(color: Color): Long =
    0xFF000000L or (color.toArgb().toLong() and 0x00FFFFFFL)

/** Finds the nearest named colour in [COLOR_CHART] by Euclidean RGB distance. */
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

// ─────────────────────────────────────────────────────────────────────────────
//  PaletteColorPicker composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Premium grid-based colour palette picker rendered entirely on a Compose Canvas.
 *
 * Layout — [COLS] × [ROWS] = 144 micro-swatches:
 *  • Column  0 : Grayscale — pure #FFFFFF → pure #000000
 *  • Column  1 : Browns / warm neutrals — cream → dark chocolate
 *  • Columns 2-15: 14 hue groups (Red, Coral, Orange, Amber, Yellow,
 *    Lime, Green, Emerald, Teal, Sky, Blue, Indigo, Purple, Pink)
 *    Each column transitions smoothly from bright/pastel (top) to deep/dark (bottom).
 *
 * Performance: all [COLS]×[ROWS] ARGB values are pre-computed once at startup
 * in [PaletteData]. The draw loop uses only value-class [Color]/[Offset]/[Size]
 * constructions — zero heap allocations per frame except two [Stroke] objects
 * for the selection ring.
 *
 * @param initialHex  Fully-opaque 0xFF______L colour to pre-select.
 * @param onColorChanged  Invoked with (argbHexLong, nearestColorName) on every selection.
 */
@Composable
fun PaletteColorPicker(
    initialHex: Long,
    onColorChanged: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Resolve the closest grid cell to the initial colour so it starts highlighted.
    val initCell = remember(initialHex) { findClosestCell(initialHex) }
    var selRow by remember { mutableIntStateOf(initCell.first) }
    var selCol by remember { mutableIntStateOf(initCell.second) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            // Responsive: cell size = totalWidth / COLS  ×  totalHeight / ROWS
            .aspectRatio(COLS.toFloat() / ROWS.toFloat())
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Handle initial finger-down
                    val down = awaitFirstDown(requireUnconsumed = false)
                    hitTest(down.position, size.width.toFloat(), size.height.toFloat()) { r, c ->
                        selRow = r; selCol = c
                        emitColor(r, c, onColorChanged)
                    }
                    down.consume()

                    // Handle drag — real-time colour updates as the finger moves
                    do {
                        val event = awaitPointerEvent()
                        val drag = event.changes.firstOrNull() ?: break
                        if (drag.pressed) {
                            hitTest(drag.position, size.width.toFloat(), size.height.toFloat()) { r, c ->
                                selRow = r; selCol = c
                                emitColor(r, c, onColorChanged)
                            }
                            drag.consume()
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val cellW = size.width / COLS
        val cellH = size.height / ROWS

        // 1-pixel gap between swatches for a clean grid aesthetic
        val gap = 1f
        val swatchW = cellW - gap
        val swatchH = cellH - gap
        val halfGap = gap * 0.5f

        // ── Draw all 144 micro-swatches ───────────────────────────────────
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                drawRect(
                    color = Color(PaletteData.ARGB[row][col]),
                    topLeft = Offset(col * cellW + halfGap, row * cellH + halfGap),
                    size = Size(swatchW, swatchH),
                )
            }
        }

        // ── Draw separator making the grayscale column prominent ──────────
        // A subtle dark line between the grayscale column (col 0) and the
        // chromatic section to visually set it apart as a dedicated section.
        drawLine(
            color = Color(0x40000000),   // 25 % opaque black
            start = Offset(cellW - halfGap, 0f),
            end = Offset(cellW - halfGap, size.height),
            strokeWidth = gap * 2,
        )

        // ── Draw selection ring ───────────────────────────────────────────
        val cx = selCol * cellW + cellW * 0.5f
        val cy = selRow * cellH + cellH * 0.5f
        val ringR = minOf(cellW, cellH) * 0.37f
        val ringW = minOf(cellW, cellH) * 0.13f

        // Adaptive colours: white ring on dark swatches, black ring on light ones
        val lum = PaletteData.LUMINANCE[selRow][selCol]
        val bright = lum > 0.52f
        val ringPrimary = if (bright) Color.Black else Color.White
        val ringShadow = if (bright) Color(0x99FFFFFF) else Color(0x99000000)
        val center = Offset(cx, cy)

        // Outer halo (shadow) for contrast against any background swatch
        drawCircle(
            color = ringShadow,
            radius = ringR + ringW * 0.65f,
            center = center,
            style = Stroke(width = ringW * 1.5f),
        )
        // Inner crisp ring
        drawCircle(
            color = ringPrimary,
            radius = ringR,
            center = center,
            style = Stroke(width = ringW),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Private helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Maps a touch position to a [row, col] grid cell, clamping to valid bounds. */
private inline fun hitTest(
    pos: Offset,
    width: Float,
    height: Float,
    onCell: (row: Int, col: Int) -> Unit,
) {
    val col = (pos.x / (width / COLS)).toInt().coerceIn(0, COLS - 1)
    val row = (pos.y / (height / ROWS)).toInt().coerceIn(0, ROWS - 1)
    onCell(row, col)
}

/** Converts a grid cell to a hex long and invokes [onColorChanged]. */
private fun emitColor(row: Int, col: Int, onColorChanged: (Long, String) -> Unit) {
    val argb = PaletteData.ARGB[row][col]
    val hex = 0xFF000000L or (argb.toLong() and 0x00FFFFFFL)
    onColorChanged(hex, closestChartColor(hex).name)
}

/** Finds the grid cell whose colour is closest to [hex] (Euclidean RGB distance). */
private fun findClosestCell(hex: Long): Pair<Int, Int> {
    val r1 = ((hex shr 16) and 0xFF).toInt()
    val g1 = ((hex shr 8) and 0xFF).toInt()
    val b1 = (hex and 0xFF).toInt()
    var bestRow = 0; var bestCol = 0; var bestDist = Int.MAX_VALUE
    for (row in 0 until ROWS) {
        for (col in 0 until COLS) {
            val a = PaletteData.ARGB[row][col]
            val r2 = (a shr 16) and 0xFF
            val g2 = (a shr 8) and 0xFF
            val b2 = a and 0xFF
            val dr = r1 - r2; val dg = g1 - g2; val db = b1 - b2
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) { bestDist = dist; bestRow = row; bestCol = col }
        }
    }
    return bestRow to bestCol
}
