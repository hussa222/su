package com.example.canvas

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.models.CanvasState
import com.example.providers.CanvasViewModel

@Composable
fun CanvasBoard(
    viewModel: CanvasViewModel,
    canvasState: CanvasState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // هنا يتم استخدام الـ filter لعرض العناصر المرئية فقط
        canvasState.elements
            .filter { it.isVisible }
            .forEach { element ->
                // استدعي المكون المسؤول عن رسم العنصر هنا
                // تأكدي من تمرير الـ element وتحديثاته
                CanvasElementComponent(
                    element = element,
                    isSelected = canvasState.selectedElementIds.contains(element.id),
                    canvasState = canvasState,
                    viewModel = viewModel
                )
            }
    }
}
