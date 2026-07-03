package com.example.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.CanvasElement
import com.example.models.CanvasState
import com.example.models.ElementType
import com.example.providers.CanvasViewModel

/** يُشارك بين اللوحة والسلايدر: صحيح أثناء سحب أي سلايدر داخل اللوحة. */
private val LocalSliderDragging = compositionLocalOf { mutableStateOf(false) }

@Composable
fun PropertiesSidebar(
    canvasState: CanvasState,
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    val activeElementId = canvasState.activePropertiesElementId
    val element = canvasState.elements.find { it.id == activeElementId }

    // حالة الطي (سهم الإخفاء/الإظهار)
    var isCollapsed by rememberSaveable { mutableStateOf(false) }

    // حالة سحب السلايدر — تُستخدم لتفعيل الشفافية على إطار اللوحة
    val draggingState = remember { mutableStateOf(false) }
    val chromeAlpha by animateFloatAsState(
        targetValue = if (draggingState.value) 0.08f else 1f,
        label = "sidebarChromeAlpha"
    )

    CollapsibleSidePanel(
        isCollapsed = isCollapsed || element == null,
        onToggleCollapsed = { isCollapsed = !isCollapsed },
        edge = PanelEdge.END,
        modifier = modifier.fillMaxHeight()
    ) {
        if (element != null) {
            CompositionLocalProvider(LocalSliderDragging provides draggingState) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = chromeAlpha)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                        ),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    tonalElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // Sidebar Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (element.type) {
                                        ElementType.TRUNK -> Icons.Default.Forest
                                        ElementType.BRANCH -> Icons.Default.Yard
                                        ElementType.LEAF -> Icons.Default.Eco
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "خصائص العنصر",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (element.type) {
                                            ElementType.TRUNK -> "جذع شجرة العائلة"
                                            ElementType.BRANCH -> "فرع العائلة الفرعي"
                                            ElementType.LEAF -> "ورقة حفيد العائلة"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.showPropertiesSidebar(null) },
                                modifier = Modifier
                                    .testTag("properties_close_button")
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق اللوحة الجانبية (Close Sidebar)",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Tab Navigation
                        var selectedTab by remember { mutableStateOf(0) }
                        val tabs = listOf("الأبعاد", "التنسيق", "النصوص", "الخصائص")
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            divider = {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Properties Scrollable Container — لا يتأثر بشفافية الإطار، يبقى واضحًا أثناء السحب
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .graphicsLayer(alpha = 1f / chromeAlpha.coerceAtLeast(0.08f) * chromeAlpha) // يبقى دائمًا 1f فعليًا
                        ) {
                            when (selectedTab) {
                                0 -> TransformTab(element = element, viewModel = viewModel)
                                1 -> StyleTab(element = element, viewModel = viewModel)
                                2 -> TextTab(element = element, viewModel = viewModel)
                                3 -> SpecificTab(element = element, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransformTab(element: CanvasElement, viewModel: CanvasViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SidebarSectionTitle(text = "الأبعاد والتحجيم (Dimensions)") }

        item {
            SidebarSliderControl(
                label = "العرض (Width)",
                value = element.width,
                range = 40f..400f,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(width = it) } },
                viewModel = viewModel,
                testTag = "property_width"
            )
        }

        item {
            SidebarSliderControl(
                label = "الارتفاع (Height)",
                value = element.height,
                range = 40f..400f,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(height = it) } },
                viewModel = viewModel,
                testTag = "property_height"
            )
        }

        item {
            SidebarSliderControl(
                label = "سمك الهيكل (Thickness)",
                value = element.thickness,
                range = 2f..30f,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(thickness = it) } },
                viewModel = viewModel,
                testTag = "property_thickness"
            )
        }

        item {
            SidebarSliderControl(
                label = "زاوية الدوران (Rotation)",
                value = element.rotation,
                range = 0f..360f,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(rotation = it) } },
                viewModel = viewModel,
                testTag = "property_rotation"
            )
        }

        item {
            SidebarSliderControl(
                label = "انحناء الفرع (Curvature)",
                value = element.curvature,
                range = -100f..100f,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(curvature = it) } },
                viewModel = viewModel,
                testTag = "property_curvature"
            )
        }

        item { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }

        item { SidebarSectionTitle(text = "الإحداثيات على اللوحة (Coordinates)") }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SidebarNumberInput(
                        label = "موقع X",
                        value = element.position.x,
                        onValueChange = { viewModel.updateElementPosition(element.id, Offset(it, element.position.y)) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SidebarNumberInput(
                        label = "موقع Y",
                        value = element.position.y,
                        onValueChange = { viewModel.updateElementPosition(element.id, Offset(element.position.x, it)) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    viewModel.updateElementWithUndo(element.id) { el ->
                        el.copy(
                            width = when (element.type) {
                                ElementType.TRUNK -> 180f
                                ElementType.BRANCH -> 150f
                                ElementType.LEAF -> 100f
                            },
                            height = when (element.type) {
                                ElementType.TRUNK -> 120f
                                ElementType.BRANCH -> 100f
                                ElementType.LEAF -> 70f
                            },
                            thickness = 8f,
                            rotation = 0f,
                            curvature = 0f
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "إعادة تعيين الأبعاد", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StyleTab(element: CanvasElement, viewModel: CanvasViewModel) {
    val presetColors = listOf(
        "#8B5A2B", "#3E2723", "#2E7D32", "#1B5E20",
        "#311B92", "#0D47A1", "#B71C1C", "#E65100"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SidebarSectionTitle(text = "ألوان المظهر والخلفية (Appearance)") }

        item {
            SidebarColorPicker(
                label = "لون العنصر الأساسي (Color)",
                hexValue = element.colorHex,
                presets = presetColors,
                onColorChange = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(colorHex = it) } }
            )
        }

        item {
            SidebarSliderControl(
                label = "الشفافية (Opacity)",
                value = element.opacity,
                range = 0.1f..1.0f,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(opacity = it) } },
                viewModel = viewModel,
                testTag = "property_opacity"
            )
        }

        item { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }

        item { SidebarSectionTitle(text = "الحدود الخارجية (Border)") }

        item {
            SidebarColorPicker(
                label = "لون الحدود (Border Color)",
                hexValue = element.borderColorHex,
                presets = listOf("#FFFFFF", "#CCCCCC", "#999999", "#8B5A2B", "#2E7D32", "#FFD54F"),
                onColorChange = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(borderColorHex = it) } }
            )
        }

        item {
            SidebarSliderControl(
                label = "سمك الحدود (Border Thickness)",
                value = element.borderThickness,
                range = 0f..10f,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(borderThickness = it) } },
                viewModel = viewModel,
                testTag = "property_border_thickness"
            )
        }

        item { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }

        item { SidebarSectionTitle(text = "ظل العنصر (Shadow)") }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "تشغيل الظل (Enable Shadow)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = element.hasShadow,
                    onCheckedChange = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(hasShadow = it) } }
                )
            }
        }

        if (element.hasShadow) {
            item {
                SidebarSliderControl(
                    label = "قوة تظليل العنصر (Elevation)",
                    value = element.shadowElevation,
                    range = 1f..24f,
                    onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(shadowElevation = it) } },
                    viewModel = viewModel,
                    testTag = "property_shadow_elevation"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    viewModel.updateElementWithUndo(element.id) { el ->
                        el.copy(
                            colorHex = when (element.type) {
                                ElementType.TRUNK -> "#8B5A2B"
                                ElementType.BRANCH -> "#3E2723"
                                ElementType.LEAF -> "#2E7D32"
                            },
                            opacity = 1.0f,
                            borderColorHex = "#FFFFFF",
                            borderThickness = 2.0f,
                            hasShadow = true,
                            shadowElevation = 4.0f
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "إعادة تعيين الألوان والمظهر", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TextTab(element: CanvasElement, viewModel: CanvasViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SidebarSectionTitle(text = "إدخال وتنسيق العناوين (Label & Text)") }

        item {
            OutlinedTextField(
                value = element.title,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(title = it) } },
                label = { Text("العنوان الرئيسي (Title)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            SidebarSliderControl(
                label = "حجم الخط (Font Size)",
                value = element.fontSize,
                range = 8f..32f,
                onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(fontSize = it) } },
                viewModel = viewModel,
                testTag = "property_font_size"
            )
        }

        item {
            SidebarColorPicker(
                label = "لون الخط الأساسي (Font Color)",
                hexValue = element.fontColorHex,
                presets = listOf("#FFFFFF", "#000000", "#FFD54F", "#81C784", "#FF8A65", "#4FC3F7"),
                onColorChange = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(fontColorHex = it) } }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "نوع الخط (Font Family)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                val families = listOf("SANS_SERIF" to "افتراضي", "SERIF" to "تقليدي (Serif)", "MONOSPACE" to "مطور (Monospace)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    families.forEach { (key, name) ->
                        val isSel = element.fontFamily == key
                        Button(
                            onClick = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(fontFamily = key) } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "محاذاة النص (Alignment)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                val alignments = listOf("Left" to Icons.Default.FormatAlignLeft, "Center" to Icons.Default.FormatAlignCenter, "Right" to Icons.Default.FormatAlignRight)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alignments.forEach { (key, icon) ->
                        val isSel = element.alignment == key
                        IconButton(
                            onClick = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(alignment = key) } },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    viewModel.updateElementWithUndo(element.id) { el ->
                        el.copy(
                            fontSize = 12f,
                            fontColorHex = "#FFFFFF",
                            fontFamily = "SANS_SERIF",
                            alignment = "Center"
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "إعادة تعيين تنسيق النصوص", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SpecificTab(element: CanvasElement, viewModel: CanvasViewModel) {
    val paddedNames = remember(element.trunkNames) {
        val names = element.trunkNames.toMutableList()
        while (names.size < 10) names.add("")
        names
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (element.type) {
            ElementType.TRUNK -> {
                item { SidebarSectionTitle(text = "قائمة أسماء العائلة الممتدة (حتى 10 أسماء)") }

                itemsIndexed(paddedNames) { index, name ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newVal ->
                            val updatedList = paddedNames.toMutableList()
                            updatedList[index] = newVal
                            viewModel.updateElement(element.id) { el -> el.copy(trunkNames = updatedList) }
                        },
                        label = { Text("الاسم ${index + 1}") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }

                item {
                    SidebarSliderControl(
                        label = "المسافة بين الأسماء (Names Spacing)",
                        value = element.trunkItemSpacing,
                        range = 2f..24f,
                        onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(trunkItemSpacing = it) } },
                        viewModel = viewModel,
                        testTag = "property_trunk_spacing"
                    )
                }
            }

            ElementType.BRANCH -> {
                item { SidebarSectionTitle(text = "تنسيق دائرة رأس الفرع (Circle Head)") }

                item {
                    SidebarSliderControl(
                        label = "قطر الدائرة (Circle Diameter)",
                        value = element.branchCircleDiameter,
                        range = 16f..80f,
                        onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(branchCircleDiameter = it) } },
                        viewModel = viewModel,
                        testTag = "property_branch_circle_diameter"
                    )
                }

                item {
                    SidebarColorPicker(
                        label = "لون الدائرة (Circle Background)",
                        hexValue = element.branchCircleColorHex,
                        presets = listOf("#81C784", "#4CAF50", "#2E7D32", "#FF8A65", "#FFD54F", "#4FC3F7"),
                        onColorChange = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(branchCircleColorHex = it) } }
                    )
                }

                item {
                    SidebarColorPicker(
                        label = "لون حدود الدائرة (Circle Border)",
                        hexValue = element.branchCircleBorderColorHex,
                        presets = listOf("#FFFFFF", "#CCCCCC", "#999999", "#8B5A2B", "#FFD54F"),
                        onColorChange = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(branchCircleBorderColorHex = it) } }
                    )
                }

                item {
                    SidebarSliderControl(
                        label = "سمك حدود الدائرة (Border Thickness)",
                        value = element.branchCircleBorderThickness,
                        range = 0f..8f,
                        onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(branchCircleBorderThickness = it) } },
                        viewModel = viewModel,
                        testTag = "property_branch_circle_border_thickness"
                    )
                }

                item {
                    SidebarColorPicker(
                        label = "لون نص الدائرة (Circle Text Color)",
                        hexValue = element.branchCircleTextColorHex,
                        presets = listOf("#FFFFFF", "#000000", "#FFD54F", "#81C784", "#4FC3F7"),
                        onColorChange = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(branchCircleTextColorHex = it) } }
                    )
                }

                item {
                    SidebarSliderControl(
                        label = "حجم نص الدائرة (Circle Text Size)",
                        value = element.branchCircleTextSize,
                        range = 6f..24f,
                        onValueChange = { viewModel.updateElement(element.id) { el -> el.copy(branchCircleTextSize = it) } },
                        viewModel = viewModel,
                        testTag = "property_branch_circle_text_size"
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "خط دائرة الرأس (Circle Font Family)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        val families = listOf("SANS_SERIF" to "افتراضي", "SERIF" to "تقليدي", "MONOSPACE" to "مطور")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            families.forEach { (key, name) ->
                                val isSel = element.branchCircleFontFamily == key
                                Button(
                                    onClick = { viewModel.updateElementWithUndo(element.id) { el -> el.copy(branchCircleFontFamily = key) } },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = name, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            ElementType.LEAF -> {
                item { SidebarSectionTitle(text = "موقع النص داخل الورقة (Label Placement)") }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "اختر موقع النص داخل الورقة الطبيعية:", style = MaterialTheme.typography.bodyMedium)
                        val positions = listOf("Center" to "الوسط", "Top" to "الأعلى", "Bottom" to "الأسفل", "Left" to "اليسار", "Right" to "اليمين")
                        positions.forEach { (key, name) ->
                            val isSel = element.leafTextPosition == key
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateElementWithUndo(element.id) { el -> el.copy(leafTextPosition = key) } }
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = name, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium)
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            SidebarSectionTitle(text = "إجراءات العنصر (Element Actions)")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isLocked = element.locked
                        Button(
                            onClick = { viewModel.lockElement(element.id, !isLocked) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isLocked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isLocked) "إلغاء القفل" else "قفل العنصر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.hideElement(element.id, false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "إخفاء العنصر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.copySelected() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "نسخ", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.cutSelected() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "قص", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.duplicateSelected() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "تكرار", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.deleteSelected() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "حذف", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SidebarSliderControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    viewModel: CanvasViewModel,
    testTag: String
) {
    var isDragging by remember { mutableStateOf(false) }
    val dragging = LocalSliderDragging.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        viewModel.saveUndoState()
                        onValueChange((value - 1f).coerceIn(range.start, range.endInclusive))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        viewModel.saveUndoState()
                        onValueChange((value + 1f).coerceIn(range.start, range.endInclusive))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        Slider(
            value = value,
            onValueChange = {
                if (!isDragging) {
                    viewModel.saveUndoState()
                    isDragging = true
                    dragging.value = true
                }
                onValueChange(it)
            },
            onValueChangeFinished = {
                isDragging = false
                dragging.value = false
            },
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SidebarNumberInput(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = if (value % 1.0f == 0.0f) value.toInt().toString() else String.format("%.1f", value),
            onValueChange = { newVal -> newVal.toFloatOrNull()?.let { onValueChange(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SidebarColorPicker(
    label: String,
    hexValue: String,
    presets: List<String>,
    onColorChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { preset ->
                val isSelected = hexValue.equals(preset, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(preset)))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorChange(preset) }
                )
            }
        }

        OutlinedTextField(
            value = hexValue,
            onValueChange = { onColorChange(it) },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            try { Color(android.graphics.Color.parseColor(hexValue)) }
                            catch (e: Exception) { Color.Transparent }
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            },
            label = { Text("رمز اللون (Hex Code)") },
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
