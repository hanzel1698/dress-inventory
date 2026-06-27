package com.hanzel.dressinventory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.view.ViewGroup
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hanzel.dressinventory.data.COLOR_CHART
import com.hanzel.dressinventory.data.ChartColor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// RGB <-> HSV Conversion Helpers
fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hsv = floatArrayOf(h, s, v)
    val argb = android.graphics.Color.HSVToColor(hsv)
    return Color(argb)
}

fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

fun findClosestColor(customHex: Long): ChartColor {
    val r1 = (customHex shr 16) and 0xFF
    val g1 = (customHex shr 8) and 0xFF
    val b1 = customHex and 0xFF

    return COLOR_CHART.minByOrNull { c ->
        val r2 = (c.hex shr 16) and 0xFF
        val g2 = (c.hex shr 8) and 0xFF
        val b2 = c.hex and 0xFF
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        dr * dr + dg * dg + db * db
    } ?: COLOR_CHART.first()
}

/**
 * Draws a gorgeous HSV color wheel where Hue maps to angle and Saturation maps to radius.
 * Brightness (Value) is applied as a dark overlay.
 */
@Composable
fun ColorWheelCanvas(
    hue: Float,
    saturation: Float,
    value: Float,
    onColorChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(4.dp, CircleShape)
            .background(Color.White, CircleShape)
            .padding(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .pointerInput(hue, saturation) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val radius = size.width / 2f
                            if (radius > 0f) {
                                val dx = offset.x - radius
                                val dy = offset.y - radius
                                val distance = sqrt(dx * dx + dy * dy)
                                val s = (distance / radius).coerceIn(0f, 1f)
                                val angleRad = atan2(dy, dx)
                                val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                                val h = (angleDeg + 360f) % 360f
                                onColorChanged(h, s)
                            }
                        },
                        onDrag = { change, _ ->
                            val radius = size.width / 2f
                            if (radius > 0f) {
                                val dx = change.position.x - radius
                                val dy = change.position.y - radius
                                val distance = sqrt(dx * dx + dy * dy)
                                val s = (distance / radius).coerceIn(0f, 1f)
                                val angleRad = atan2(dy, dx)
                                val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                                val h = (angleDeg + 360f) % 360f
                                onColorChanged(h, s)
                            }
                        }
                    )
                }
                .pointerInput(hue, saturation) {
                    detectTapGestures { offset ->
                        val radius = size.width / 2f
                        if (radius > 0f) {
                            val dx = offset.x - radius
                            val dy = offset.y - radius
                            val distance = sqrt(dx * dx + dy * dy)
                            val s = (distance / radius).coerceIn(0f, 1f)
                            val angleRad = atan2(dy, dx)
                            val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                            val h = (angleDeg + 360f) % 360f
                            onColorChanged(h, s)
                        }
                    }
                }
        ) {
            val radius = size.width / 2f
            val center = Offset(radius, radius)

            // 1. Draw SweepGradient for Hue
            val sweepBrush = Brush.sweepGradient(
                colors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                ),
                center = center
            )
            drawCircle(brush = sweepBrush, radius = radius, center = center)

            // 2. Draw RadialGradient for Saturation (white in center, transparent at outer edge)
            val radialBrush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            )
            drawCircle(brush = radialBrush, radius = radius, center = center)

            // 3. Draw Black Overlay for Value (Brightness)
            drawCircle(color = Color.Black, radius = radius, center = center, alpha = (1f - value).coerceIn(0f, 1f))

            // 4. Draw cursor
            val angleRad = Math.toRadians(hue.toDouble())
            val cursorDistance = saturation * radius
            val cursorX = radius + cursorDistance * cos(angleRad).toFloat()
            val cursorY = radius + cursorDistance * sin(angleRad).toFloat()

            // Outer white ring
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(cursorX, cursorY),
                style = Stroke(width = 3.dp.toPx())
            )
            // Inner black ring
            drawCircle(
                color = Color.Black,
                radius = 10.dp.toPx(),
                center = Offset(cursorX, cursorY),
                style = Stroke(width = 1.dp.toPx())
            )
            // Inner color preview
            val cursorColor = hsvToColor(hue, saturation, value)
            drawCircle(
                color = cursorColor,
                radius = 8.dp.toPx(),
                center = Offset(cursorX, cursorY)
            )
        }
    }
}

/**
 * A beautiful interactive Color Wheel preview for forms.
 * Triggers a fullscreen zoom fine-tuning overlay when clicked.
 * Clicking on the solid color circle displays a list of curated starting shades.
 */
@Composable
fun ColorWheelPicker(
    initialColorHex: Long,
    onColorSelected: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFullscreen by remember { mutableStateOf(false) }
    var showBaseColorDialog by remember { mutableStateOf(false) }
    val initialColor = Color(initialColorHex)
    val (initialH, initialS, initialV) = remember(initialColorHex) { colorToHsv(initialColor) }

    // Display the local preview
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { showFullscreen = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tiny representation of the Color Wheel
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.width / 2f
                    val center = Offset(radius, radius)

                    val sweepBrush = Brush.sweepGradient(
                        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                        center = center
                    )
                    drawCircle(brush = sweepBrush, radius = radius, center = center)

                    val radialBrush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius
                    )
                    drawCircle(brush = radialBrush, radius = radius, center = center)
                    drawCircle(color = Color.Black, radius = radius, center = center, alpha = (1f - initialV).coerceIn(0f, 1f))

                    // Small indicator ring for the selected color
                    val angleRad = Math.toRadians(initialH.toDouble())
                    val cursorDistance = initialS * radius
                    val cursorX = radius + cursorDistance * cos(angleRad).toFloat()
                    val cursorY = radius + cursorDistance * sin(angleRad).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(cursorX, cursorY),
                        style = Stroke(width = 1.5f.dp.toPx())
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                val chartColor = findClosestColor(initialColorHex)
                Text(
                    text = "${chartColor.name} (Fine-tuned)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("#%06X", (0xFFFFFFL and initialColorHex).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap wheel to zoom & fine-tune ☀️",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Big color preview block - Tapping this opens the curated outfit colors dialog
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(initialColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { showBaseColorDialog = true }
            )
        }
    }

    if (showBaseColorDialog) {
        Dialog(onDismissRequest = { showBaseColorDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Curated Outfit Colours",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select a common base shade to start, then fine-tune it in natural light.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val columns = 4
                            val rows = COLOR_CHART.chunked(columns)
                            rows.forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowColors.forEach { c ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(60.dp)
                                                .clickable {
                                                    onColorSelected(c.hex, c.name)
                                                    showBaseColorDialog = false
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(c.hex))
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                        CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = c.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    if (rowColors.size < columns) {
                                        repeat(columns - rowColors.size) {
                                            Spacer(modifier = Modifier.width(60.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = { showBaseColorDialog = false }
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    if (showFullscreen) {
        FullscreenColorWheelDialog(
            initialHue = initialH,
            initialSaturation = initialS,
            initialValue = initialV,
            onDismiss = { showFullscreen = false },
            onConfirm = { h, s, v ->
                val chosenColor = hsvToColor(h, s, v)
                val hexValue = 0xFF000000L or (chosenColor.toArgb().toLong() and 0xFFFFFFFFL)
                val closestName = findClosestColor(hexValue).name
                onColorSelected(hexValue, closestName)
                showFullscreen = false
            }
        )
    }
}

/**
 * Fullscreen dialog offering natural light fine tuning with a rectangular zoomed view
 * of the color wheel and a large borderless physical matching swatch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenColorWheelDialog(
    initialHue: Float,
    initialSaturation: Float,
    initialValue: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float, Float, Float) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        LaunchedEffect(dialogWindowProvider) {
            dialogWindowProvider?.window?.let { window ->
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }

        var hue by remember { mutableFloatStateOf(initialHue) }
        var saturation by remember { mutableFloatStateOf(initialSaturation) }
        var value by remember { mutableFloatStateOf(initialValue) }

        val currentColor = hsvToColor(hue, saturation, value)
        val currentHex = 0xFF000000L or (currentColor.toArgb().toLong() and 0xFFFFFFFFL)
        val closestChartColor = findClosestColor(currentHex)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Solid Color Matching Swatch (Top 58% of screen, completely borderless, goes edge-to-edge)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(currentColor)
                ) {
                    // Floating overlay card for HUD and info, placed away from the screen borders
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.75f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Natural Light Swatch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Place the top of your screen near the outfit.\nDrag on the spectrum pad below to match.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(currentColor)
                                        .border(1.dp, Color.White, CircleShape)
                                )
                                Text(
                                    text = "${closestChartColor.name} (${String.format("#%06X", (0xFFFFFFL and currentHex).toInt())})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 2. Fullscreen Rectangular Zoomed View of the Colour Wheel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "Zoomed Colour Wheel Spectrum",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(initialHue, initialSaturation) {
                                    detectDragGestures { change, _ ->
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        if (w > 0f && h > 0f) {
                                            val tX = (change.position.x / w).coerceIn(0f, 1f)
                                            val tY = (change.position.y / h).coerceIn(0f, 1f)

                                            val hRange = 80f
                                            val sRange = 0.5f
                                            val H_min_rel = initialHue - hRange / 2f
                                            val hRel = H_min_rel + tX * hRange
                                            hue = (hRel + 360f) % 360f

                                            val S_min = (initialSaturation - sRange / 2f).coerceIn(0f, 1f)
                                            val S_max = (initialSaturation + sRange / 2f).coerceIn(0f, 1f)
                                            saturation = (S_max - tY * (S_max - S_min)).coerceIn(0f, 1f)
                                        }
                                    }
                                }
                                .pointerInput(initialHue, initialSaturation) {
                                    detectTapGestures { offset ->
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        if (w > 0f && h > 0f) {
                                            val tX = (offset.x / w).coerceIn(0f, 1f)
                                            val tY = (offset.y / h).coerceIn(0f, 1f)

                                            val hRange = 80f
                                            val sRange = 0.5f
                                            val H_min_rel = initialHue - hRange / 2f
                                            val hRel = H_min_rel + tX * hRange
                                            hue = (hRel + 360f) % 360f

                                            val S_min = (initialSaturation - sRange / 2f).coerceIn(0f, 1f)
                                            val S_max = (initialSaturation + sRange / 2f).coerceIn(0f, 1f)
                                            saturation = (S_max - tY * (S_max - S_min)).coerceIn(0f, 1f)
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            val hRange = 80f
                            val sRange = 0.5f
                            val H_min_rel = initialHue - hRange / 2f
                            val S_min = (initialSaturation - sRange / 2f).coerceIn(0f, 1f)
                            val S_max = (initialSaturation + sRange / 2f).coerceIn(0f, 1f)

                            // Draw horizontal linear Hue gradient
                            val steps = 8
                            val gradientColors = List(steps) { index ->
                                val fraction = index.toFloat() / (steps - 1)
                                val currentH = (H_min_rel + fraction * hRange + 360f) % 360f
                                hsvToColor(currentH, 1f, 1f)
                            }
                            val hueBrush = Brush.horizontalGradient(gradientColors)
                            drawRect(brush = hueBrush)

                            // Blend vertical Saturation gradient (white overlay)
                            val whiteBrush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 1f - S_max),
                                    Color.White.copy(alpha = 1f - S_min)
                                )
                            )
                            drawRect(brush = whiteBrush)

                            // Apply Black Value Overlay (Brightness)
                            drawRect(color = Color.Black, alpha = (1f - value).coerceIn(0f, 1f))

                            // Draw cursor pointing to current selected hue and saturation
                            val hFraction = ((hue - H_min_rel + 360f) % 360f) / hRange
                            val sFraction = if (S_max != S_min) (S_max - saturation) / (S_max - S_min) else 0.5f

                            val cursorX = (hFraction * w).coerceIn(0f, w)
                            val cursorY = (sFraction * h).coerceIn(0f, h)

                            // Draw cursor ring
                            drawCircle(
                                color = Color.White,
                                radius = 14.dp.toPx(),
                                center = Offset(cursorX, cursorY),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 12.dp.toPx(),
                                center = Offset(cursorX, cursorY),
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color = currentColor,
                                radius = 10.dp.toPx(),
                                center = Offset(cursorX, cursorY)
                            )
                        }
                    }
                }

                // 3. Compact Bottom HUD (Luminance Slider & Action Buttons)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 44.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Brightness slider compact row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Brightness5, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Luminance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = value,
                            onValueChange = { value = it },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Text(
                            text = "${(value * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(44.dp)
                        )
                    }

                    // Done & Cancel buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = { onConfirm(hue, saturation, value) },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Shade")
                        }
                    }
                }
            }
        }
    }
}

private fun isLight(argb: Long): Boolean {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b) > 160
}
