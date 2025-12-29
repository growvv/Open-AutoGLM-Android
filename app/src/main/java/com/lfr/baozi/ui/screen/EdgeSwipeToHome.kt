package com.lfr.baozi.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun EdgeSwipeToHome(
    enabled: Boolean,
    onSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    if (!enabled) {
        content(modifier)
        return
    }

    val density = LocalDensity.current
    val edgePx = remember(density) { with(density) { 24.dp.toPx() } }
    val triggerPx = remember(density) { with(density) { 84.dp.toPx() } }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var settleJob: Job? by remember { mutableStateOf(null) }

    content(
        modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width }
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(enabled, widthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > edgePx) return@awaitEachGesture

                    settleJob?.cancel()
                    var totalDy = 0f
                    var lastPos = down.position
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                        if (!change.pressed) break

                        val dx = change.position.x - lastPos.x
                        val dy = change.position.y - lastPos.y
                        totalDy += dy
                        lastPos = change.position

                        val max = widthPx.toFloat().takeIf { it > 0 } ?: Float.MAX_VALUE
                        val next = (offsetX + dx).coerceIn(0f, max)
                        offsetX = next
                        change.consume()
                    }

                    val shouldSwipe = offsetX > triggerPx && abs(totalDy) < triggerPx
                    val target =
                        if (shouldSwipe) widthPx.toFloat().takeIf { it > 0 } ?: (triggerPx * 2)
                        else 0f

                    settleJob =
                        scope.launch {
                            val start = offsetX
                            animate(
                                initialValue = start,
                                targetValue = target,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            ) { value, _ ->
                                offsetX = value
                            }

                            if (shouldSwipe) {
                                onSwipe()
                                offsetX = 0f
                            }
                        }
                }
            }
    )
}
