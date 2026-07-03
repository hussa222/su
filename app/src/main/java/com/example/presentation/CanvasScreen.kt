package com.example.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.canvas.CanvasBoard
import com.example.providers.CanvasViewModel
import com.example.ui.components.LayerManagerSidebar

@Composable
fun CanvasScreen(viewModel: CanvasViewModel) {
    // 1. جلب حالة التطبيق من الـ ViewModel
    val canvasState by viewModel.state.collectAsState()

    // 2. ترتيب الواجهة: قائمة الطبقات بجانب لوحة الرسم
    Row(modifier = Modifier.fillMaxSize()) {
        
        // استدعاء القائمة الجانبية التي أنشأناها
        LayerManagerSidebar(
            viewModel = viewModel, 
            canvasState = canvasState
        )
        
        // استدعاء لوحة الرسم التي تحتوي على منطق الإخفاء
        CanvasBoard(
            viewModel = viewModel, 
            canvasState = canvasState, 
            modifier = Modifier.weight(1f)
        )
    }
}
