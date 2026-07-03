package com.example.canvas.engine

import androidx.compose.ui.geometry.Offset
import com.example.models.CanvasElement
import com.example.models.ElementType
import com.example.providers.getAnchorsForType
import kotlin.math.sqrt

object SmartTreeEngine {

    private const val SNAP_DISTANCE = 35f

    data class SnapResult(
        val parentId: String,
        val anchorId: String,
        val position: Offset
    )

    fun findSnapTarget(
        moving: CanvasElement,
        elements: List<CanvasElement>
    ): SnapResult? {

        var best: SnapResult? = null
        var bestDistance = Float.MAX_VALUE

        for (parent in elements) {

            if (parent.id == moving.id) continue

            if (!canConnect(parent.type, moving.type)) continue

            val anchors = getAnchorsForType(
                parent.type,
                parent.width,
                parent.height
            )

            for (anchor in anchors) {

                if (anchor.isInput) continue

                val anchorPosition = parent.position + anchor.relativePos

                val dx = anchorPosition.x - moving.position.x
                val dy = anchorPosition.y - moving.position.y

                val distance = sqrt(dx * dx + dy * dy)

                if (distance < SNAP_DISTANCE && distance < bestDistance) {

                    bestDistance = distance

                    best = SnapResult(
                        parentId = parent.id,
                        anchorId = anchor.id,
                        position = anchorPosition
                    )
                }
            }
        }

        return best
    }

    fun canConnect(
        parent: ElementType,
        child: ElementType
    ): Boolean {

        return when (parent) {

            ElementType.TRUNK ->
                child == ElementType.BRANCH

            ElementType.BRANCH ->
                child == ElementType.BRANCH ||
                        child == ElementType.LEAF

            ElementType.LEAF ->
                false
        }
    }
}
