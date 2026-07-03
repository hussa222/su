package com.example.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.canvas.CanvasBoard
import com.example.models.ElementType
import com.example.providers.CanvasViewModel
import com.example.widgets.ControlPanel
import com.example.widgets.CoordinatePanel
import com.example.widgets.PropertiesSidebar

/**
 * CanvasScreen hosts the interactive workspace.
 * Combines the CanvasBoard, ControlPanel, CoordinatePanel, Floating Creative Tools Bar, and the floating header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    canvasViewModel: CanvasViewModel = viewModel()
) {
    val canvasState by canvasViewModel.state.collectAsState()

    // Track viewport size to center zoom steps
    var viewportWidth by remember { mutableStateOf(1080f) }
    var viewportHeight by remember { mutableStateOf(1920f) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. The Core Canvas Engine
        CanvasBoard(
            viewModel = canvasViewModel,
            canvasState = canvasState,
            onSizeChanged = { w, h ->
                viewportWidth = w
                viewportHeight = h
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Floating Header Bar (Back navigation + Undo/Redo + Auto Layout)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { canvasViewModel.undo() },
                        enabled = canvasState.canUndo,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "تراجع (Undo)",
                            tint = if (canvasState.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = { canvasViewModel.redo() },
                        enabled = canvasState.canRedo,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cached,
                            contentDescription = "إعادة (Redo)",
                            tint = if (canvasState.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = { canvasViewModel.triggerAutoLayout() },
                        modifier = Modifier.testTag("auto_layout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "ترتيب تلقائي (Auto Layout)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 3. Zoom Control Panel (left side, vertically centered)
        ControlPanel(
            onZoomIn = { center -> canvasViewModel.zoomIn(center) },
            onZoomOut = { center -> canvasViewModel.zoomOut(center) },
            onReset = { canvasViewModel.resetCanvas() },
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            scale = canvasState.scale,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )

        // 4. Creative Element Toolbox (Floating panel above Coordinates)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp) // Position nicely elevated above coordinates
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multi-select Mode Toggle
                val isMulti = canvasState.isMultiSelectMode
                IconButton(
                    onClick = { canvasViewModel.toggleMultiSelectMode() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMulti) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "تحديد متعدد (Multi-Select)",
                        tint = if (isMulti) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Tool 1: Add Trunk
                TextButton(
                    onClick = {
                        val randomOffset = Offset((-100..100).random().toFloat(), (-100..100).random().toFloat())
                        canvasViewModel.addElement(ElementType.TRUNK, randomOffset)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF8B5A2B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Forest,
                        contentDescription = "إضافة جذع عائلة",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ جذع",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Tool 2: Add Branch
                TextButton(
                    onClick = {
                        val randomOffset = Offset((-100..100).random().toFloat(), (-100..100).random().toFloat())
                        canvasViewModel.addElement(ElementType.BRANCH, randomOffset)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF5D4037)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Yard,
                        contentDescription = "إضافة فرع",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ فرع",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Tool 3: Add Leaf
                TextButton(
                    onClick = {
                        val randomOffset = Offset((-100..100).random().toFloat(), (-100..100).random().toFloat())
                        canvasViewModel.addElement(ElementType.LEAF, randomOffset)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = "إضافة ورقة عائلة",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ ورقة",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // 5. Coordinate Panel (bottom center, very bottom edge)
        CoordinatePanel(
            canvasState = canvasState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )

        // 6. Professional Properties Sidebar (Phase 5)
        PropertiesSidebar(
            canvasState = canvasState,
            viewModel = canvasViewModel,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // 7. Error Message Banner (Smart Rules Engine)
        canvasState.errorMessage?.let { errorMsg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 90.dp, start = 24.dp, end = 24.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(
                            onClick = { canvasViewModel.clearErrorMessage() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // 9. Safe Deletion Confirmation Dialog (Trunk)
        if (canvasState.pendingDeleteTrunkConfirmation) {
            AlertDialog(
                onDismissRequest = { canvasViewModel.cancelDeleteTrunk() },
                confirmButton = {
                    TextButton(
                        onClick = { canvasViewModel.deleteSelected(confirmed = true) }
                    ) {
                        Text(text = "تأكيد الحذف الكامل", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { canvasViewModel.cancelDeleteTrunk() }
                    ) {
                        Text(text = "إلغاء")
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(text = "تأكيد حذف الجذع الرئيسي")
                },
                text = {
                    Text(text = "تحذير هام: حذف الجذع سيؤدي لحذف كافة الفروع والأوراق المرتبطة به. هل أنت متأكد من الحذف؟")
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
