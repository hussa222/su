package com.example.canvas

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.CanvasElement
import com.example.models.CanvasState
import com.example.models.ElementType
import com.example.providers.CanvasViewModel
import com.example.providers.getAnchorsForType
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * CanvasBoard is the high-performance drawing board component.
 * Uses math-bounded mathematical grid rendering and GPU graphicsLayer translation.
 * Features Phase 4 Smart Family Tree Engine: Anchors drawing, natural wooden branches connection,
 * recursive branch/leaf child moving, and snapping.
 */
@Composable
fun CanvasBoard(
    viewModel: CanvasViewModel,
    canvasState: CanvasState,
    modifier: Modifier = Modifier,
    onSizeChanged: (width: Float, height: Float) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Extract theme colors
    val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }

    val gridBackground = if (isLightTheme) {
        Color(0xFFF9FAF5) // soft organic slate-ivory
    } else {
        Color(0xFF0F1115) // deep night forest
    }

    val minorGridColor = if (isLightTheme) {
        Color(0xFFE5EAE2).copy(alpha = 0.5f)
    } else {
        Color(0xFF22262B).copy(alpha = 0.5f)
    }

    val majorGridColor = if (isLightTheme) {
        Color(0xFFCCD5CA)
    } else {
        Color(0xFF2D3339)
    }

    val xAxisColor = Color(0xFF8B5A2B).copy(alpha = 0.25f) // Subtle trunk brown axis
    val yAxisColor = Color(0xFF2E7D32).copy(alpha = 0.25f) // Subtle leaf green axis

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(gridBackground)
            .onSizeChanged { size ->
                // Keeps viewport size in sync with every layout change
                // (screen rotation, sidebars opening/closing, etc.)
                onSizeChanged(size.width.toFloat(), size.height.toFloat())
            }
            .pointerInput(Unit) {
                // Multi-pointer gesture loop
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for the first pointer to go down
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downScreenPos = down.position

                            // Fetch fresh state values
                            val currentState = viewModel.state.value
                            val scale = currentState.scale
                            val offset = currentState.offset

                            // Project screen touch into canvas space
                            val canvasX = (downScreenPos.x - offset.x) / scale
                            val canvasY = (downScreenPos.y - offset.y) / scale
                            val canvasPosDp = Offset(canvasX / density.density, canvasY / density.density)

                            // Hit test elements
                            val hitElement = viewModel.findElementAt(canvasPosDp)
                            var activeDraggedId: String? = null

                            // Start a coroutine to detect long press
                            var longPressJob: Job? = null

                            if (hitElement != null) {
                                // Hit element: Select it and set as the active dragged element
                                viewModel.selectElement(hitElement.id)
                                activeDraggedId = hitElement.id

                                longPressJob = launch {
                                    delay(500)
                                    // Trigger soft haptic vibration
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Open properties sidebar
                                    viewModel.showPropertiesSidebar(hitElement.id)
                                }
                            } else {
                                // Hit empty space: Clear selection (if not in multi-select mode) and pan canvas
                                if (!currentState.isMultiSelectMode) {
                                    viewModel.deselectAll()
                                }
                                activeDraggedId = null
                            }

                            var totalDragDistance = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val activeChanges = event.changes.filter { it.pressed }

                                if (activeChanges.isEmpty()) {
                                    longPressJob?.cancel()
                                    viewModel.clearTouch()
                                    break
                                }

                                val activePointers = activeChanges.count()
                                if (activePointers == 1) {
                                    val change = activeChanges[0]
                                    if (change.previousPressed) {
                                        val panDelta = change.position - change.previousPosition
                                        totalDragDistance += panDelta.getDistance()

                                        // If touch moves beyond threshold, cancel the long press detection
                                        if (totalDragDistance > 15f) {
                                            longPressJob?.cancel()
                                        }

                                        val latestState = viewModel.state.value

                                        if (activeDraggedId != null) {
                                            // Drag Element Mode: Move element recursively
                                            val deltaDp = Offset(
                                                (panDelta.x / latestState.scale) / density.density,
                                                (panDelta.y / latestState.scale) / density.density
                                            )
                                            viewModel.dragElement(activeDraggedId, deltaDp)
                                        } else {
                                            // Pan Canvas Mode
                                            viewModel.updatePan(panDelta)
                                        }
                                    }
                                    viewModel.updateTouchPosition(change.position, 1)
                                } else if (activePointers >= 2) {
                                    longPressJob?.cancel()
                                    activeDraggedId = null

                                    val change1 = activeChanges[0]
                                    val change2 = activeChanges[1]

                                    if (change1.previousPressed && change2.previousPressed) {
                                        val curPos1 = change1.position
                                        val curPos2 = change2.position
                                        val prevPos1 = change1.previousPosition
                                        val prevPos2 = change2.previousPosition

                                        val centroid = (curPos1 + curPos2) / 2f
                                        val prevCentroid = (prevPos1 + prevPos2) / 2f
                                        val panDelta = centroid - prevCentroid

                                        val curDist = (curPos1 - curPos2).getDistance()
                                        val prevDist = (prevPos1 - prevPos2).getDistance()

                                        val zoomFactor = if (prevDist > 0f) curDist / prevDist else 1.0f

                                        viewModel.updateTransform(zoomFactor, panDelta, centroid)
                                        viewModel.updateTouchPosition(centroid, activePointers)
                                    } else {
                                        val centroid = (activeChanges[0].position + activeChanges[1].position) / 2f
                                        viewModel.updateTouchPosition(centroid, activePointers)
                                    }
                                }

                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Draw mathematical infinite grid in background
        Canvas(modifier = Modifier.fillMaxSize().testTag("infinite_grid")) {
            drawInfiniteGrid(
                width = widthPx,
                height = heightPx,
                scale = canvasState.scale,
                offset = canvasState.offset,
                gridBackground = gridBackground,
                minorGridColor = minorGridColor,
                majorGridColor = majorGridColor,
                xAxisColor = xAxisColor,
                yAxisColor = yAxisColor,
                density = density.density
            )
        }

        // 2. Transformed elements container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = canvasState.scale
                    scaleY = canvasState.scale
                    translationX = canvasState.offset.x
                    translationY = canvasState.offset.y
                }
        ) {
            // Draw natural organic wooden connections behind elements
            Canvas(modifier = Modifier.fillMaxSize()) {
                val densityVal = density.density
                canvasState.elements.forEach { child ->
                    val parentId = child.parentId
                    val parentAnchorId = child.parentAnchorId
                    if (parentId != null && parentAnchorId != null) {
                        val parent = canvasState.elements.find { it.id == parentId }
                        if (parent != null) {
                            val parentAnchors = getAnchorsForType(parent.type, parent.width, parent.height)
                            val parentAnchor = parentAnchors.find { it.id == parentAnchorId }
                            
                            val childInputAnchors = getAnchorsForType(child.type, child.width, child.height)
                            val childInputAnchor = childInputAnchors.find { it.isInput }
                            
                            if (parentAnchor != null && childInputAnchor != null) {
                                val startPx = (parent.position + parentAnchor.relativePos) * densityVal
                                val endPx = (child.position + childInputAnchor.relativePos) * densityVal
                                
                                // Direct control offset based on normal direction
                                val normal = when (parentAnchor.id) {
                                    "left_1", "left_2" -> Offset(-1f, 0.2f)
                                    "right_1", "right_2" -> Offset(1f, 0.2f)
                                    "branch_left" -> Offset(-0.8f, -0.6f)
                                    "branch_right" -> Offset(0.8f, -0.6f)
                                    "leaf_top" -> Offset(0f, -1f)
                                    else -> Offset(0f, -1f)
                                }
                                
                                val controlOffsetPx1 = normal * (45f * densityVal)
                                val controlOffsetPx2 = Offset(0f, 40f * densityVal) // enters child from base below
                                
                                val cp1 = startPx + controlOffsetPx1
                                val cp2 = endPx + controlOffsetPx2
                                
                                val path = Path().apply {
                                    moveTo(startPx.x, startPx.y)
                                    cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, endPx.x, endPx.y)
                                }
                                
                                // Draw beautiful tapering natural wood branches
                                drawPath(
                                    path = path,
                                    color = Color(0xFF6D4C41), // Rich brown wood bark
                                    style = Stroke(
                                        width = 8f * densityVal,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                                
                                // Inner vein detail
                                drawPath(
                                    path = path,
                                    color = Color(0xFF8D6E63), // Light heartwood vein
                                    style = Stroke(
                                        width = 2.5f * densityVal,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                                
                                // Green leaf buds along the curve to add incredible fidelity
                                val tPoints = listOf(0.35f, 0.7f)
                                tPoints.forEach { t ->
                                    val mt = 1f - t
                                    val px = mt*mt*mt*startPx.x + 3*mt*mt*t*cp1.x + 3*mt*t*t*cp2.x + t*t*t*endPx.x
                                    val py = mt*mt*mt*startPx.y + 3*mt*mt*t*cp1.y + 3*mt*t*t*cp2.y + t*t*t*endPx.y
                                    
                                    drawCircle(
                                        color = Color(0xFF4CAF50),
                                        radius = 4.5f * densityVal,
                                        center = Offset(px, py)
                                    )
                                    drawCircle(
                                        color = Color(0xFF81C784),
                                        radius = 2f * densityVal,
                                        center = Offset(px - 1.5f * densityVal, py - 1.5f * densityVal)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Render elements
            canvasState.elements.forEach { element ->
                val isSelected = canvasState.selectedElementIds.contains(element.id)

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                ((element.position.x - element.width / 2f) * density.density).roundToInt(),
                                ((element.position.y - element.height / 2f) * density.density).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            rotationZ = element.rotation
                            alpha = element.opacity
                        }
                ) {
                    CanvasElementComponent(
                        element = element,
                        isSelected = isSelected,
                        canvasState = canvasState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

/**
 * Renders a single family tree element with high-fidelity adaptive themes and anchors overlay.
 */
@Composable
fun CanvasElementComponent(
    element: CanvasElement,
    isSelected: Boolean,
    canvasState: CanvasState,
    viewModel: CanvasViewModel
) {
    val elementColor = try {
        Color(android.graphics.Color.parseColor(element.colorHex))
    } catch (e: Exception) {
        Color(0xFF8B5A2B)
    }

    // Infinite transition for active target snapping glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(element.width.dp, element.height.dp)
    ) {
        // Core visual contents
        when (element.type) {
            ElementType.TRUNK -> TrunkContent(element, elementColor)
            ElementType.BRANCH -> BranchContent(element, elementColor)
            ElementType.LEAF -> LeafContent(element, elementColor)
        }

        // Selected highlights & anchors
        if (isSelected) {
            // Figma-style high precision selection outline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = when (element.type) {
                            ElementType.LEAF -> RoundedCornerShape(topStart = 24.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 24.dp)
                            ElementType.BRANCH -> RoundedCornerShape(12.dp)
                            else -> RoundedCornerShape(16.dp)
                        }
                    )
            )

            // Render all anchor points only on the selected element
            val anchors = getAnchorsForType(element.type, element.width, element.height)
            anchors.forEach { anchor ->
                val isTarget = canvasState.activeSnapTarget?.parentId == element.id &&
                               canvasState.activeSnapTarget?.anchorId == anchor.id
                
                // Position anchor point relative to element box center
                val anchorX = element.width / 2f + anchor.relativePos.x
                val anchorY = element.height / 2f + anchor.relativePos.y

                Box(
                    modifier = Modifier
                        .offset(
                            x = (anchorX - 10f).dp, // offset to center the anchor dot (width/2)
                            y = (anchorY - 10f).dp
                        )
                        .size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTarget) {
                        // Snapping target hover glow pulse
                        Box(
                            modifier = Modifier
                                .size((18f * pulseScale).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32).copy(alpha = 0.4f))
                        )
                    }

                    // Anchor Dot representation
                    Box(
                        modifier = Modifier
                            .size(if (isTarget) 14.dp else 10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTarget) Color(0xFF00E676)
                                else if (anchor.isInput) Color(0xFFFF9800) // Input is Amber
                                else Color(0xFF00E5FF) // Output is Neon Cyan
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Floating quick action menu directly above the element
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-44).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE61C1B1F)) // bg-[#1C1B1F]/90
                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lock Action
                val isLocked = element.locked
                IconButton(
                    onClick = { viewModel.lockElement(element.id, !isLocked) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "قفل/فتح (Lock/Unlock)",
                        tint = if (isLocked) Color(0xFFFFD54F) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 16.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Hide Action
                IconButton(
                    onClick = { viewModel.hideElement(element.id, false) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "إخفاء (Hide)",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 16.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Copy Action
                IconButton(
                    onClick = { viewModel.copySelected() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ (Copy)",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 16.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Cut Action
                IconButton(
                    onClick = { viewModel.cutSelected() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "قص (Cut)",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 16.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Duplicate Action
                IconButton(
                    onClick = { viewModel.duplicateSelected() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "تكرار (Duplicate)",
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 16.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Delete Action
                IconButton(
                    onClick = { viewModel.deleteSelected() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف (Delete)",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Utility functions for color and typography formatting
 */
private fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

private fun mapFontFamily(name: String): FontFamily {
    return when (name) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "SANS_SERIF" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
}

private fun mapTextAlign(align: String): TextAlign {
    return when (align) {
        "Left" -> TextAlign.Left
        "Right" -> TextAlign.Right
        else -> TextAlign.Center
    }
}

/**
 * Solid wood log visual container for the Trunk element.
 */
@Composable
private fun TrunkContent(element: CanvasElement, color: Color) {
    val textMeasurer = rememberTextMeasurer()
    val opacity = element.opacity
    val trunkColor = parseHexColor(element.colorHex, color).copy(alpha = parseHexColor(element.colorHex, color).alpha * opacity)
    val borderColor = parseHexColor(element.borderColorHex, Color.White).copy(alpha = parseHexColor(element.borderColorHex, Color.White).alpha * opacity)
    val fontColor = parseHexColor(element.fontColorHex, Color.White).copy(alpha = parseHexColor(element.fontColorHex, Color.White).alpha * opacity)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Organic Trunk Path with Roots and wavy/bumpy tree trunk lines
        val mainPath = Path().apply {
            moveTo(w * 0.15f, h * 0.95f)
            quadraticTo(w * 0.05f, h * 0.98f, w * 0.02f, h)
            lineTo(w * 0.12f, h)
            quadraticTo(w * 0.5f, h * 0.88f, w * 0.88f, h)
            lineTo(w * 0.98f, h)
            quadraticTo(w * 0.95f, h * 0.98f, w * 0.85f, h * 0.95f)
            cubicTo(w * 0.78f, h * 0.80f, w * 0.84f, h * 0.65f, w * 0.78f, h * 0.55f)
            cubicTo(w * 0.72f, h * 0.45f, w * 0.76f, h * 0.30f, w * 0.72f, h * 0.18f)
            quadraticTo(w * 0.68f, h * 0.08f, w * 0.64f, 0f)
            quadraticTo(w * 0.5f, h * 0.04f, w * 0.36f, 0f)
            quadraticTo(w * 0.32f, h * 0.08f, w * 0.28f, h * 0.18f)
            cubicTo(w * 0.24f, h * 0.30f, w * 0.28f, h * 0.45f, w * 0.22f, h * 0.55f)
            cubicTo(w * 0.16f, h * 0.65f, w * 0.22f, h * 0.80f, w * 0.15f, h * 0.95f)
            close()
        }

        // Draw Vector Shadow
        if (element.hasShadow) {
            val shadowOffset = element.shadowElevation.dp.toPx()
            val shadowPath = Path().apply {
                addPath(mainPath, Offset(shadowOffset, shadowOffset))
            }
            drawPath(
                path = shadowPath,
                color = Color.Black.copy(alpha = 0.12f * opacity)
            )
        }

        // Draw Main Trunk Fill (Gradient)
        drawPath(
            path = mainPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    trunkColor,
                    trunkColor.copy(
                        red = trunkColor.red * 0.60f,
                        green = trunkColor.green * 0.60f,
                        blue = trunkColor.blue * 0.60f
                    )
                )
            )
        )

        // Draw Border Outline
        if (element.borderThickness > 0) {
            drawPath(
                path = mainPath,
                color = borderColor,
                style = Stroke(width = element.borderThickness.dp.toPx())
            )
        }

        // Draw Wood Grain lines (ألياف الخشب)
        clipPath(mainPath) {
            for (grainXRatio in listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.7f)) {
                val grainPath = Path().apply {
                    moveTo(w * grainXRatio, h)
                    cubicTo(
                        w * (grainXRatio - 0.05f), h * 0.66f,
                        w * (grainXRatio + 0.05f), h * 0.33f,
                        w * (grainXRatio - 0.02f), 0f
                    )
                }
                drawPath(
                    path = grainPath,
                    color = Color.Black.copy(alpha = 0.08f * opacity),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Draw headline text centered near the top
        val titleStyle = TextStyle(
            color = fontColor,
            fontSize = (element.fontSize + 2).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = mapFontFamily(element.fontFamily),
            textAlign = TextAlign.Center
        )
        val titleLayout = textMeasurer.measure(element.title, style = titleStyle)
        val titleX = (w - titleLayout.size.width) / 2f
        val titleY = h * 0.08f // Header top padding relative spacing

        drawText(
            textMeasurer = textMeasurer,
            text = element.title,
            topLeft = Offset(titleX, titleY),
            style = titleStyle
        )

        // Draw 10 names vertically distributed (parametric placement)
        val namesList = element.trunkNames.toMutableList()
        while (namesList.size < 10) {
            namesList.add("")
        }

        val nameStyle = TextStyle(
            color = fontColor.copy(alpha = 0.95f),
            fontSize = element.fontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = mapFontFamily(element.fontFamily),
            textAlign = TextAlign.Center
        )

        val startY = h * 0.22f
        val endY = h * 0.88f
        val stepY = (endY - startY) / 9f

        for (i in 0..9) {
            val name = namesList[9 - i] // Draw 9 (topmost slot) down to 0 (bottommost slot)
            val currentY = startY + i * stepY
            if (name.isNotBlank()) {
                val measured = textMeasurer.measure(name, style = nameStyle)
                val nameX = (w - measured.size.width) / 2f
                val nameY = currentY - measured.size.height / 2f
                drawText(
                    textMeasurer = textMeasurer,
                    text = name,
                    topLeft = Offset(nameX, nameY),
                    style = nameStyle
                )
            } else {
                // Draw a beautiful horizontal wood line representing an empty slot (ألياف الخشب)
                val lineW = w * 0.15f
                drawLine(
                    color = fontColor.copy(alpha = 0.15f),
                    start = Offset((w - lineW) / 2f, currentY),
                    end = Offset((w + lineW) / 2f, currentY),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

/**
 * Branch banner visual design for the Branch element.
 */
@Composable
private fun BranchContent(element: CanvasElement, color: Color) {
    val textMeasurer = rememberTextMeasurer()
    val opacity = element.opacity
    val branchColor = parseHexColor(element.colorHex, color).copy(alpha = parseHexColor(element.colorHex, color).alpha * opacity)
    val borderColor = parseHexColor(element.borderColorHex, Color.White).copy(alpha = parseHexColor(element.borderColorHex, Color.White).alpha * opacity)
    val fontColor = parseHexColor(element.fontColorHex, Color.White).copy(alpha = parseHexColor(element.fontColorHex, Color.White).alpha * opacity)
    
    val circleColor = parseHexColor(element.branchCircleColorHex, Color(0xFF81C784)).copy(alpha = parseHexColor(element.branchCircleColorHex, Color(0xFF81C784)).alpha * opacity)
    val circleBorderColor = parseHexColor(element.branchCircleBorderColorHex, Color.White).copy(alpha = parseHexColor(element.branchCircleBorderColorHex, Color.White).alpha * opacity)
    val circleTextColor = parseHexColor(element.branchCircleTextColorHex, Color.White).copy(alpha = parseHexColor(element.branchCircleTextColorHex, Color.White).alpha * opacity)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val circleRadius = element.branchCircleDiameter.dp.toPx() / 2f
        
        // The circle head is centered at top-center, slightly offset down to fit inside bounds
        val circleCenter = Offset(w / 2f, circleRadius + element.branchCircleBorderThickness.dp.toPx() + 4f)
        
        // Adjust curvature using the curvature percentage
        val curveOffset = (element.curvature / 100f) * (w * 0.35f)
        
        // Calculate taper thicknesses (thick base, thin tip connection)
        val baseThickness = element.thickness.dp.toPx() * 1.6f
        val tipThickness = element.thickness.dp.toPx() * 0.45f
        
        // 1. Tapered Branch Stem Path (thick base at connection point, thin at circle head)
        val stemPath = Path().apply {
            // Bottom Left of the thick base
            moveTo(w / 2f - baseThickness / 2f, h)
            
            // Left curve going up to the tip connection using cubic Bezier
            cubicTo(
                w / 2f - baseThickness / 2f + curveOffset, h * 0.66f,
                circleCenter.x - tipThickness / 2f + curveOffset, h * 0.33f,
                circleCenter.x - tipThickness / 2f, circleCenter.y + circleRadius
            )
            
            // Top edge connecting to the circle
            lineTo(circleCenter.x + tipThickness / 2f, circleCenter.y + circleRadius)
            
            // Right curve going back down to the base using cubic Bezier
            cubicTo(
                circleCenter.x + tipThickness / 2f + curveOffset, h * 0.33f,
                w / 2f + baseThickness / 2f + curveOffset, h * 0.66f,
                w / 2f + baseThickness / 2f, h
            )
            
            close()
        }
        
        // Draw Stem Shadow
        if (element.hasShadow) {
            val shadowOffset = element.shadowElevation.dp.toPx()
            val shadowPath = Path().apply {
                moveTo(w / 2f - baseThickness / 2f + shadowOffset, h + shadowOffset)
                cubicTo(
                    w / 2f - baseThickness / 2f + curveOffset + shadowOffset, h * 0.66f + shadowOffset,
                    circleCenter.x - tipThickness / 2f + curveOffset + shadowOffset, h * 0.33f + shadowOffset,
                    circleCenter.x - tipThickness / 2f + shadowOffset, circleCenter.y + circleRadius + shadowOffset
                )
                lineTo(circleCenter.x + tipThickness / 2f + shadowOffset, circleCenter.y + circleRadius + shadowOffset)
                cubicTo(
                    circleCenter.x + tipThickness / 2f + curveOffset + shadowOffset, h * 0.33f + shadowOffset,
                    w / 2f + baseThickness / 2f + curveOffset + shadowOffset, h * 0.66f + shadowOffset,
                    w / 2f + baseThickness / 2f + shadowOffset, h + shadowOffset
                )
                close()
            }
            drawPath(
                path = shadowPath,
                color = Color.Black.copy(alpha = 0.12f * opacity)
            )
        }
        
        // Draw Stem Fill (Gradient)
        drawPath(
            path = stemPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    branchColor,
                    branchColor.copy(
                        red = branchColor.red * 0.60f,
                        green = branchColor.green * 0.60f,
                        blue = branchColor.blue * 0.60f
                    )
                )
            )
        )
        
        // Draw Stem Border Outline
        if (element.borderThickness > 0) {
            drawPath(
                path = stemPath,
                color = borderColor,
                style = Stroke(width = element.borderThickness.dp.toPx())
            )
        }
        
        // Draw multiple light organic wood lines following the curvature inside the branch
        clipPath(stemPath) {
            for (offsetRatio in listOf(-0.25f, 0f, 0.25f)) {
                val grainPath = Path().apply {
                    val baseShift = baseThickness * 0.3f * offsetRatio
                    val tipShift = tipThickness * 0.3f * offsetRatio
                    moveTo(w / 2f + baseShift, h)
                    cubicTo(
                        w / 2f + baseShift + curveOffset, h * 0.66f,
                        circleCenter.x + tipShift + curveOffset, h * 0.33f,
                        circleCenter.x + tipShift, circleCenter.y + circleRadius
                    )
                }
                drawPath(
                    path = grainPath,
                    color = Color.White.copy(alpha = 0.15f * opacity),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
        
        // 2. Draw Branch Circle Node at the head
        // Draw Circle Shadow
        if (element.hasShadow) {
            val shadowOffset = element.shadowElevation.dp.toPx()
            drawCircle(
                color = Color.Black.copy(alpha = 0.12f * opacity),
                radius = circleRadius,
                center = circleCenter + Offset(shadowOffset, shadowOffset)
            )
        }
        
        // Draw Circle Fill
        drawCircle(
            color = circleColor,
            radius = circleRadius,
            center = circleCenter
        )
        
        // Draw Circle Border
        if (element.branchCircleBorderThickness > 0) {
            drawCircle(
                color = circleBorderColor,
                radius = circleRadius,
                center = circleCenter,
                style = Stroke(width = element.branchCircleBorderThickness.dp.toPx())
            )
        }

        // Draw text inside Circle Head
        val circleText = if (element.title.length > 5) element.title.take(5) else element.title
        val headStyle = TextStyle(
            color = circleTextColor,
            fontSize = element.branchCircleTextSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = mapFontFamily(element.branchCircleFontFamily),
            textAlign = TextAlign.Center
        )
        val measuredHead = textMeasurer.measure(circleText, style = headStyle)
        val headX = circleCenter.x - measuredHead.size.width / 2f
        val headY = circleCenter.y - measuredHead.size.height / 2f

        drawText(
            textMeasurer = textMeasurer,
            text = circleText,
            topLeft = Offset(headX, headY),
            style = headStyle
        )

        // Draw secondary branch label below the circle if name is long
        if (element.title.length > 5) {
            val labelStyle = TextStyle(
                color = fontColor,
                fontSize = (element.fontSize - 1).coerceAtLeast(8f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = mapFontFamily(element.fontFamily),
                textAlign = mapTextAlign(element.alignment)
            )
            val measuredLabel = textMeasurer.measure(element.title, style = labelStyle)
            val labelX = (w - measuredLabel.size.width) / 2f
            val labelY = h - measuredLabel.size.height - 8.dp.toPx()

            drawText(
                textMeasurer = textMeasurer,
                text = element.title,
                topLeft = Offset(labelX, labelY),
                style = labelStyle
            )
        }
    }
}

/**
 * Organic leaf shape visual design for Leaf elements.
 */
@Composable
private fun LeafContent(element: CanvasElement, color: Color) {
    val textMeasurer = rememberTextMeasurer()
    val opacity = element.opacity
    val leafColor = parseHexColor(element.colorHex, color).copy(alpha = parseHexColor(element.colorHex, color).alpha * opacity)
    val borderColor = parseHexColor(element.borderColorHex, Color(0x40FFFFFF)).copy(alpha = parseHexColor(element.borderColorHex, Color(0x40FFFFFF)).alpha * opacity)
    val fontColor = parseHexColor(element.fontColorHex, Color.White).copy(alpha = parseHexColor(element.fontColorHex, Color.White).alpha * opacity)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val curveOffset = (element.curvature / 100f) * (w * 0.35f)
        
        // 1. Organic Asymmetric Leaf Path starting from the top tip (رأس الورقة) to the bottom
        val leafPath = Path().apply {
            moveTo(w / 2f + curveOffset, h * 0.05f)
            quadraticTo(w * 0.95f + curveOffset, h * 0.32f, w / 2f, h * 0.90f)
            lineTo(w / 2f, h)
            lineTo(w / 2f, h * 0.90f)
            quadraticTo(w * 0.05f + curveOffset, h * 0.42f, w / 2f + curveOffset, h * 0.05f)
            close()
        }

        // Draw Leaf Shadow
        if (element.hasShadow) {
            val shadowOffset = element.shadowElevation.dp.toPx()
            val shadowPath = Path().apply {
                addPath(leafPath, Offset(shadowOffset, shadowOffset))
            }
            drawPath(
                path = shadowPath,
                color = Color.Black.copy(alpha = 0.12f * opacity)
            )
        }

        // Draw Leaf Fill (Natural Forest Gradient)
        drawPath(
            path = leafPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    leafColor,
                    leafColor.copy(
                        red = leafColor.red * 0.60f,
                        green = leafColor.green * 0.60f,
                        blue = leafColor.blue * 0.60f
                    )
                )
            )
        )

        // Draw Leaf Border Outline
        if (element.borderThickness > 0) {
            drawPath(
                path = leafPath,
                color = borderColor,
                style = Stroke(width = element.borderThickness.dp.toPx())
            )
        }

        // Draw beautiful leaf veins clipped to the shape
        clipPath(leafPath) {
            // Central midrib vein (العرق الأوسط)
            val midribPath = Path().apply {
                moveTo(w / 2f, h * 0.90f)
                quadraticTo(
                    w / 2f + curveOffset, h * 0.5f,
                    w / 2f + curveOffset, h * 0.05f
                )
            }
            drawPath(
                path = midribPath,
                color = Color.White.copy(alpha = 0.25f * opacity),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5 lateral veins branching out (العروق الجانبية)
            for (i in 1..5) {
                val progress = i * 0.15f + 0.1f // from 0.25 to 0.85
                val startX = w / 2f + curveOffset * (1f - progress)
                val startY = h * (0.90f - progress * 0.85f)
                
                // Left lateral vein (curves down and out)
                val leftVein = Path().apply {
                    moveTo(startX, startY)
                    quadraticTo(
                        startX - w * 0.18f, startY + h * 0.05f,
                        startX - w * 0.32f, startY - h * 0.08f
                    )
                }
                drawPath(
                    path = leftVein,
                    color = Color.White.copy(alpha = 0.18f * opacity),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Right lateral vein (curves down and out)
                val rightVein = Path().apply {
                    moveTo(startX, startY)
                    quadraticTo(
                        startX + w * 0.18f, startY + h * 0.05f,
                        startX + w * 0.32f, startY - h * 0.08f
                    )
                }
                drawPath(
                    path = rightVein,
                    color = Color.White.copy(alpha = 0.18f * opacity),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Centered text inside the leaf drawn directly on Canvas
        val leafStyle = TextStyle(
            color = fontColor,
            fontSize = element.fontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = mapFontFamily(element.fontFamily),
            textAlign = mapTextAlign(element.alignment)
        )
        val measuredLeaf = textMeasurer.measure(element.title, style = leafStyle)
        
        val alignOffset = when (element.leafTextPosition) {
            "Top" -> Offset(0f, -h * 0.18f)
            "Bottom" -> Offset(0f, h * 0.18f)
            "Left" -> Offset(-w * 0.15f, 0f)
            "Right" -> Offset(w * 0.15f, 0f)
            else -> Offset.Zero
        }

        val textX = (w - measuredLeaf.size.width) / 2f + alignOffset.x
        val textY = (h - measuredLeaf.size.height) / 2f + alignOffset.y

        drawText(
            textMeasurer = textMeasurer,
            text = element.title,
            topLeft = Offset(textX, textY),
            style = leafStyle
        )
    }
}

/**
 * Draws the infinite-looking designer grid based on current pan & zoom.
 * Calculates visible bounds mathematically so that we only loop over lines that are on screen.
 */
private fun DrawScope.drawInfiniteGrid(
    width: Float,
    height: Float,
    scale: Float,
    offset: Offset,
    gridBackground: Color,
    minorGridColor: Color,
    majorGridColor: Color,
    xAxisColor: Color,
    yAxisColor: Color,
    density: Float
) {
    val baseCellSize = 40f * density
    val currentCellSize = baseCellSize * scale

    val startXIndex = kotlin.math.floor(-offset.x / currentCellSize).toInt()
    val endXIndex = kotlin.math.ceil((width - offset.x) / currentCellSize).toInt()

    val startYIndex = kotlin.math.floor(-offset.y / currentCellSize).toInt()
    val endYIndex = kotlin.math.ceil((height - offset.y) / currentCellSize).toInt()

    // 1. Draw vertical grid lines
    for (i in startXIndex..endXIndex) {
        val x = i * currentCellSize + offset.x
        val isMajor = i % 5 == 0

        if (i == 0) {
            drawLine(
                color = yAxisColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 3f * density
            )
        } else {
            val lineColor = if (isMajor) majorGridColor else minorGridColor
            val strokeWidth = if (isMajor) 1f * density else 0.5f * density
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = strokeWidth
            )
        }
    }

    // 2. Draw horizontal grid lines
    for (j in startYIndex..endYIndex) {
        val y = j * currentCellSize + offset.y
        val isMajor = j % 5 == 0

        if (j == 0) {
            drawLine(
                color = xAxisColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 3f * density
            )
        } else {
            val lineColor = if (isMajor) majorGridColor else minorGridColor
            val strokeWidth = if (isMajor) 1f * density else 0.5f * density
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = strokeWidth
            )
        }
    }
}
