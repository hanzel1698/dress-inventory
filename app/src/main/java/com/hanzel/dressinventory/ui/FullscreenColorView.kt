package com.hanzel.dressinventory.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

/**
 * Fullscreen colour view opened from the palette picker preview strip.
 *
 * - Fills the screen with [hex].
 * - Shows [name] and the hex code prominently at the top.
 * - Long-press on the name copies it to the clipboard.
 * - Long-press on the hex code copies it to the clipboard.
 * - Tap anywhere else to dismiss.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleColorFullscreen(
    hex: Long,
    name: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val hexString = "#%06X".format((hex and 0xFFFFFFL).toInt())

    val view = LocalView.current
    val activity = remember(view.context) { view.context.findActivity() }

    DisposableEffect(activity) {
        val ctrl = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        ctrl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val light = remember(hex) { isLight(hex) }
    val onColor = if (light) Color(0xFF1C1C1E) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(hex))
            // Dismiss on tap — child combinedClickable areas will intercept their own taps
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        // ── Top: colour name + hex value ─────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = onColor,
                modifier = Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClick = {
                        clipboard.setText(AnnotatedString(name))
                        Toast.makeText(context, "Copied \"$name\"", Toast.LENGTH_SHORT).show()
                    },
                    onClick = {},   // consume tap — prevents dismiss triggering
                ),
            )
            Text(
                text = hexString,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                color = onColor.copy(alpha = 0.68f),
                modifier = Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClick = {
                        clipboard.setText(AnnotatedString(hexString))
                        Toast.makeText(context, "Copied $hexString", Toast.LENGTH_SHORT).show()
                    },
                    onClick = {},   // consume tap — prevents dismiss triggering
                ),
            )
        }

        // ── Bottom: usage hint ────────────────────────────────────────────
        Text(
            text = "Tap to close  ·  Long-press name or # to copy",
            style = MaterialTheme.typography.bodySmall,
            color = onColor.copy(alpha = 0.40f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        )
    }
}

@Composable
fun FullscreenColorView(
    colors: List<Pair<Long, String>>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    if (colors.isEmpty()) {
        onClose()
        return
    }

    val view = LocalView.current
    val context = view.context
    val activity = remember(context) { context.findActivity() }
    val window = activity?.window

    // Hide/show system UI bars
    DisposableEffect(window, view) {
        val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        windowInsetsController?.let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(initialIndex) }
    var isNext by remember { mutableStateOf(true) }
    var offsetX by remember { mutableStateOf(0f) }
    var showOverlay by remember { mutableStateOf(true) }
    var overlayTimerKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(overlayTimerKey, currentIndex) {
        showOverlay = true
        delay(2500)
        showOverlay = false
    }

    AnimatedContent(
        targetState = currentIndex,
        transitionSpec = {
            if (isNext) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) togetherWith
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) togetherWith
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
            }
        },
        label = "ColorSlideAnimation",
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(colors) {
                detectTapGestures(
                    onTap = { onClose() }
                )
            }
            .pointerInput(colors) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        offsetX = 0f
                    },
                    onDragEnd = {
                        if (offsetX > 150f) {
                            // Swipe Right -> previous item
                            isNext = false
                            currentIndex = (currentIndex - 1 + colors.size) % colors.size
                            overlayTimerKey++
                        } else if (offsetX < -150f) {
                            // Swipe Left -> next item
                            isNext = true
                            currentIndex = (currentIndex + 1) % colors.size
                            overlayTimerKey++
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) { targetIndex ->
        // Bound targetIndex safely just in case colors list changed
        val safeIndex = if (targetIndex in colors.indices) targetIndex else 0
        val (colorHex, colorName) = colors[safeIndex]
        val isLightColor = remember(colorHex) { isLight(colorHex) }
        val textColor = if (isLightColor) {
            Color(0xFF1C1C1E).copy(alpha = 0.5f)
        } else {
            Color(0xFFFAFAF7).copy(alpha = 0.6f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(colorHex)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = showOverlay,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = colorName,
                        style = MaterialTheme.typography.displayMedium,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Swipe to cycle · Tap anywhere to return",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = textColor.alpha * 0.7f)
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun isLight(hex: Long): Boolean {
    val r = (hex shr 16) and 0xFF
    val g = (hex shr 8) and 0xFF
    val b = hex and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b) > 160
}
