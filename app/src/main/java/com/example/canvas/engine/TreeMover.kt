package com.example.canvas.engine

import androidx.compose.ui.geometry.Offset
import com.example.models.CanvasElement

object TreeMover {

    fun moveTree(
        id: String,
        delta: Offset,
        elements: MutableList<CanvasElement>
    ) {

        val index = elements.indexOfFirst { it.id == id }
        if (index == -1) return

        val current = elements[index]

        elements[index] = current.copy(
            position = current.position + delta
        )

        val children = elements.filter { it.parentId == id }

        children.forEach {
            moveTree(
                it.id,
                delta,
                elements
            )
        }
    }
}
