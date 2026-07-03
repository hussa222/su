package com.example.models

import androidx.compose.ui.geometry.Offset

data class CanvasState(
    val elements: List<CanvasElement> = emptyList(),
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val selectedElementIds: Set<String> = emptySet()
)

data class CanvasElement(
    val id: String,
    val type: ElementType,
    val title: String,
    val position: Offset,
    val width: Float,
    val height: Float,
    val isVisible: Boolean = true // هذه الخاصية هي مفتاح الحل
)

enum class ElementType { TRUNK, BRANCH, LEAF }
