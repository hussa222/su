package com.example.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.models.CanvasState
import com.example.providers.CanvasViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CanvasBoard(
    viewModel: CanvasViewModel,
    canvasState: CanvasState,
    modifier: Modifier = Modifier,
    onSizeChanged: (width: Float, height: Float) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                onSizeChanged(size.width.toFloat(), size.height.toFloat())
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val currentState = viewModel.state.value
                            
                            // تحويل دقيق لإحداثيات اللمس
                            val canvasPos = Offset(
                                (down.position.x - currentState.offset.x) / (currentState.scale * density),
                                (down.position.y - currentState.offset.y) / (currentState.scale * density)
                            )

                            val hitElement = viewModel.findElementAt(canvasPos)
                            var activeDraggedId: String? = null
                            val job = launch {
                                delay(500)
                                if (hitElement != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.showPropertiesSidebar(hitElement.id)
                                }
                            }

                            if (hitElement != null) {
                                viewModel.selectElement(hitElement.id)
                                activeDraggedId = hitElement.id
                            } else {
                                viewModel.deselectAll()
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes.filter { it.pressed }
                                if (changes.isEmpty()) {
                                    job.cancel()
                                    viewModel.clearTouch()
                                    break
                                }

                                if (changes.size == 1) {
                                    val change = changes[0]
                                    if (activeDraggedId != null) {
                                        val delta = Offset(
                                            (change.position.x - change.previousPosition.x) / (currentState.scale * density),
                                            (change.position.y - change.previousPosition.y) / (currentState.scale * density)
                                        )
                                        viewModel.dragElement(activeDraggedId, delta)
                                    } else {
                                        viewModel.updatePan(change.position - change.previousPosition)
                                    }
                                } else {
                                    job.cancel()
                                    // منطق الزووم
                                    val centroid = (changes[0].position + changes[1].position) / 2f
                                    viewModel.updateTransform(1.0f, (changes[0].position - changes[0].previousPosition), centroid)
                                }
                                changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
    ) {
        // طبقة العناصر مع الـ Transform الصحيح
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = canvasState.scale
                    scaleY = canvasState.scale
                    translationX = canvasState.offset.x
                    translationY = canvasState.offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            // هنا يتم استدعاء رسم العناصر (Trunk, Branch, Leaf)
            canvasState.elements.forEach { element ->
                Box(modifier = Modifier.offset {
                    IntOffset(
                        ((element.position.x - element.width / 2f) * density).roundToInt(),
                        ((element.position.y - element.height / 2f) * density).roundToInt()
                    )
                }) {
                    CanvasElementComponent(element, canvasState.selectedElementIds.contains(element.id), canvasState, viewModel)
                }
            }
        }
    }
}

