package com.example.open_autoglm_android.ui.screen

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

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

    content(
        modifier.pointerInput(Unit) {
            var startedFromEdge = false
            var totalDx = 0f
            var totalDy = 0f
            detectDragGestures(
                onDragStart = { offset ->
                    startedFromEdge = offset.x <= edgePx
                    totalDx = 0f
                    totalDy = 0f
                },
                onDrag = { change, dragAmount ->
                    if (!startedFromEdge) return@detectDragGestures
                    totalDx += dragAmount.x
                    totalDy += dragAmount.y
                    change.consume()
                },
                onDragCancel = {
                    startedFromEdge = false
                },
                onDragEnd = {
                    if (startedFromEdge && totalDx > triggerPx && abs(totalDy) < triggerPx) {
                        onSwipe()
                    }
                    startedFromEdge = false
                }
            )
        }
    )
}
