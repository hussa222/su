package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.models.CanvasState
import com.example.providers.CanvasViewModel

@Composable
fun LayerManagerSidebar(viewModel: CanvasViewModel, canvasState: CanvasState) {
    Card(modifier = Modifier.width(200.dp).fillMaxHeight().padding(8.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "الطبقات", style = MaterialTheme.typography.titleMedium)
            Divider()
            LazyColumn {
                items(canvasState.elements) { element ->
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = element.title, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.toggleVisibility(element.id) }) {
                            Icon(
                                imageVector = if (element.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}
