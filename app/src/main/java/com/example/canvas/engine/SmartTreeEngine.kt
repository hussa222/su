package com.example.canvas.engine

import androidx.compose.ui.geometry.Offset
import com.example.models.CanvasElement
import com.example.models.ElementType
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

            if (!canConnect(parent.type, moving.type))
                continue

            for (anchor in parent.anchors) {

                if (anchor.isOccupied)
                    continue

                val dx = anchor.position.x - moving.position.x
                val dy = anchor.position.y - moving.position.y

                val distance = sqrt(dx * dx + dy * dy)

                if (distance < SNAP_DISTANCE &&
                    distance < bestDistance
                ) {

                    bestDistance = distance

                    best = SnapResult(
                        parent.id,
                        anchor.id,
                        anchor.position
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