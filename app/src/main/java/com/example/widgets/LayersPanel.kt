package com.example.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.CanvasElement
import com.example.models.CanvasState
import com.example.models.ElementType
import com.example.providers.CanvasViewModel

/**
 * لوحة الطبقات: تعرض كل عناصر اللوحة مرتّبة حسب layerIndex وتتيح لكل عنصر:
 * تحديد، إظهار/إخفاء، قفل/فتح، ترتيب لأعلى أو لأسفل.
 */
@Composable
fun LayersPanel(
    canvasState: CanvasState,
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    val orderedElements = remember(canvasState.elements) {
        canvasState.elements.sortedByDescending { it.layerIndex }
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 300.dp)
            .fillMaxWidth(0.75f),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 12.dp,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الطبقات (${orderedElements.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (orderedElements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "لا توجد عناصر على اللوحة بعد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orderedElements, key = { it.id }) { element ->
                        val index = orderedElements.indexOf(element)
                        LayerRow(
                            element = element,
                            isSelected = canvasState.activePropertiesElementId == element.id ||
                                canvasState.selectedElementIds.contains(element.id),
                            canMoveUp = index > 0,
                            canMoveDown = index < orderedElements.lastIndex,
                            onSelect = { viewModel.showPropertiesSidebar(element.id) },
                            onToggleVisibility = { viewModel.hideElement(element.id, !element.visible) },
                            onToggleLock = { viewModel.lockElement(element.id, !element.locked) },
                            onMoveUp = {
                                if (index > 0) swapLayerIndex(orderedElements[index], orderedElements[index - 1], viewModel)
                            },
                            onMoveDown = {
                                if (index < orderedElements.lastIndex) swapLayerIndex(orderedElements[index], orderedElements[index + 1], viewModel)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun swapLayerIndex(a: CanvasElement, b: CanvasElement, viewModel: CanvasViewModel) {
    val aIndex = a.layerIndex
    val bIndex = b.layerIndex
    viewModel.updateElementWithUndo(a.id) { it.copy(layerIndex = bIndex) }
    viewModel.updateElementWithUndo(b.id) { it.copy(layerIndex = aIndex) }
}

@Composable
private fun LayerRow(
    element: CanvasElement,
    isSelected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        try { Color(android.graphics.Color.parseColor(element.colorHex)) }
                        catch (e: Exception) { MaterialTheme.colorScheme.secondaryContainer }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (element.type) {
                        ElementType.TRUNK -> Icons.Default.Forest
                        ElementType.BRANCH -> Icons.Default.Yard
                        ElementType.LEAF -> Icons.Default.Eco
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = element.title.ifBlank { "بدون عنوان" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = "طبقة ${element.layerIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(30.dp)) {
                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "لأعلى", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(30.dp)) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "لأسفل", modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onToggleVisibility, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = if (element.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "إظهار/إخفاء",
                    tint = if (element.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onToggleLock, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = if (element.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "قفل/فتح",
                    tint = if (element.locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
