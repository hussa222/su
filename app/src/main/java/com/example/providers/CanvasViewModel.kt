package com.example.providers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.CanvasElement
import com.example.models.CanvasState
import com.example.models.ElementType
import com.example.models.SnapTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Extension to map an Offset using a 4x4 graphics Matrix.
 */
fun Matrix.mapOffset(offset: Offset): Offset {
    val x = offset.x
    val y = offset.y
    val m = this.values
    
    val rx = m[0] * x + m[4] * y + m[12]
    val ry = m[1] * x + m[5] * y + m[13]
    val rw = m[3] * x + m[7] * y + m[15]
    
    val w = if (rw != 0f) rw else 1f
    return Offset(rx / w, ry / w)
}

/**
 * Anchor Point definition for connecting elements.
 */
data class AnchorPoint(
    val id: String,
    val relativePos: Offset,      // Relative to element center in DP
    val compatibleType: ElementType, // What can connect to this anchor
    val label: String,
    val isInput: Boolean          // True if input (e.g. base of Branch/Leaf), False if output
)

/**
 * Retrieves the anchors list for a specific element type and size.
 */
fun getAnchorsForType(type: ElementType, width: Float, height: Float): List<AnchorPoint> {
    return when (type) {
        ElementType.TRUNK -> listOf(
            AnchorPoint("left_1", Offset(-width / 2f, -height / 4f), ElementType.BRANCH, "فرع أيسر علوي", isInput = false),
            AnchorPoint("left_2", Offset(-width / 2f, height / 4f), ElementType.BRANCH, "فرع أيسر سفلي", isInput = false),
            AnchorPoint("right_1", Offset(width / 2f, -height / 4f), ElementType.BRANCH, "فرع أيمن علوي", isInput = false),
            AnchorPoint("right_2", Offset(width / 2f, height / 4f), ElementType.BRANCH, "فرع أيمن سفلي", isInput = false)
        )
        ElementType.BRANCH -> listOf(
            AnchorPoint("input", Offset(0f, height / 2f), ElementType.BRANCH, "رابط رئيسي للفرع", isInput = true),
            AnchorPoint("branch_left", Offset(-width / 2f, -height / 4f), ElementType.BRANCH, "فرع فرعي أيسر", isInput = false),
            AnchorPoint("branch_right", Offset(width / 2f, -height / 4f), ElementType.BRANCH, "فرع فرعي أيمن", isInput = false),
            AnchorPoint("leaf_top", Offset(0f, -height / 2f), ElementType.LEAF, "رابط الورقة", isInput = false)
        )
        ElementType.LEAF -> listOf(
            AnchorPoint("input", Offset(0f, height / 2f), ElementType.LEAF, "رابط الورقة", isInput = true)
        )
    }
}

/**
 * State manager for the Canvas Engine.
 * Responsible for handling transform math (zoom, pan, coordinate projections)
 * and elements interaction (addition, drag, selection, snap, delete)
 * with robust bounds and smooth transitions.
 */
class CanvasViewModel : ViewModel() {

    private val _state = MutableStateFlow(CanvasState())
    val state: StateFlow<CanvasState> = _state.asStateFlow()

    // Undo & Redo History Stacks
    private val undoStack = mutableListOf<List<CanvasElement>>()
    private val redoStack = mutableListOf<List<CanvasElement>>()

    // Clipboard for Cut, Copy, Paste
    private var clipboard = emptyList<CanvasElement>()

    fun saveUndoState() {
        if (undoStack.size >= 50) {
            undoStack.removeAt(0)
        }
        undoStack.add(_state.value.elements)
        redoStack.clear()
        updateUndoRedoState()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_state.value.elements)
            _state.update { current ->
                current.copy(elements = previous)
            }
            updateUndoRedoState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_state.value.elements)
            _state.update { current ->
                current.copy(elements = next)
            }
            updateUndoRedoState()
        }
    }

    private fun updateUndoRedoState() {
        _state.update { current ->
            current.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun copySelected() {
        val selectedIds = _state.value.selectedElementIds
        clipboard = _state.value.elements.filter { selectedIds.contains(it.id) }.map { original ->
            original.copy()
        }
    }

    fun cutSelected() {
        copySelected()
        deleteSelected()
    }

    fun pasteSelected() {
        if (clipboard.isEmpty()) return
        saveUndoState()
        _state.update { current ->
            val newElements = mutableListOf<CanvasElement>()
            val newIds = mutableSetOf<String>()
            val idMapping = mutableMapOf<String, String>()
            
            // First pass: copy elements and assign fresh IDs
            clipboard.forEach { original ->
                val newId = "element_paste_${System.currentTimeMillis()}_${original.hashCode() % 100}"
                idMapping[original.id] = newId
                val pasted = original.copy(
                    id = newId,
                    position = original.position + Offset(30f, 30f),
                    selected = true,
                    parentId = null,
                    parentAnchorId = null,
                    relativeOffset = Offset.Zero
                )
                newElements.add(pasted)
                newIds.add(newId)
            }
            
            // Second pass: reconnect pasted parent/children if both were copied together
            val finalPastedElements = newElements.map { pasted ->
                val original = clipboard.find { idMapping[it.id] == pasted.id }
                if (original?.parentId != null && idMapping.containsKey(original.parentId)) {
                    pasted.copy(
                        parentId = idMapping[original.parentId],
                        parentAnchorId = original.parentAnchorId,
                        relativeOffset = original.relativeOffset
                    )
                } else {
                    pasted
                }
            }
            
            val unselectedOld = current.elements.map { el -> el.copy(selected = false) }
            val combinedList = unselectedOld + finalPastedElements
            
            current.copy(
                elements = syncChildrenIds(combinedList),
                selectedElementIds = newIds
            )
        }
    }

    fun lockElement(id: String, locked: Boolean) {
        saveUndoState()
        updateElement(id) { it.copy(locked = locked) }
    }

    fun hideElement(id: String, visible: Boolean) {
        saveUndoState()
        updateElement(id) { it.copy(visible = visible) }
        if (!visible) {
            _state.update { current ->
                val newSelection = current.selectedElementIds.toMutableSet()
                newSelection.remove(id)
                current.copy(
                    selectedElementIds = newSelection,
                    elements = current.elements.map { el ->
                        if (el.id == id) el.copy(selected = false) else el
                    }
                )
            }
        }
    }

    fun showAllElements() {
        saveUndoState()
        _state.update { current ->
            current.copy(
                elements = current.elements.map { it.copy(visible = true) }
            )
        }
    }

    fun updateElementWithUndo(id: String, transform: (CanvasElement) -> CanvasElement) {
        saveUndoState()
        updateElement(id, transform)
    }

    private fun syncChildrenIds(elements: List<CanvasElement>): List<CanvasElement> {
        return elements.map { parent ->
            val children = elements.filter { it.parentId == parent.id }.map { it.id }
            parent.copy(childrenIds = children)
        }
    }

    private fun isLockedOrAncestorLocked(elementId: String, elements: List<CanvasElement>): Boolean {
        val element = elements.find { it.id == elementId } ?: return false
        if (element.locked) return true
        if (element.parentId != null) {
            return isLockedOrAncestorLocked(element.parentId, elements)
        }
        return false
    }

    // Config bounds as requested
    private val minScale = 0.2f
    private val maxScale = 10.0f

    init {
        // Populate canvas with a beautiful initial connected Family Tree as Phase 4 default!
        _state.update { current ->
            val trunk = CanvasElement(
                id = "trunk_main",
                type = ElementType.TRUNK,
                title = "جذع الأجداد (الأصل)",
                position = Offset(0f, 150f),
                width = 180f,
                height = 120f,
                colorHex = "#8B5A2B"
            )

            // Left Branch connected to Trunk's "left_1" anchor (relative offset: -90f, -30f)
            val branchLeft = CanvasElement(
                id = "branch_left_1",
                type = ElementType.BRANCH,
                title = "فرع الأبناء (العائلة الأولى)",
                position = Offset(-90f, 70f),
                width = 150f,
                height = 100f,
                colorHex = "#3E2723",
                parentId = "trunk_main",
                parentAnchorId = "left_1",
                relativeOffset = Offset(-90f, -80f)
            )

            // Right Branch connected to Trunk's "right_1" anchor (relative offset: 90f, -30f)
            val branchRight = CanvasElement(
                id = "branch_right_1",
                type = ElementType.BRANCH,
                title = "فرع الأبناء (العائلة الثانية)",
                position = Offset(90f, 70f),
                width = 150f,
                height = 100f,
                colorHex = "#3E2723",
                parentId = "trunk_main",
                parentAnchorId = "right_1",
                relativeOffset = Offset(90f, -80f)
            )

            // Leaf connected to branchLeft's "leaf_top" anchor (relative offset: 0f, -50f)
            val leafLeft = CanvasElement(
                id = "leaf_left_1",
                type = ElementType.LEAF,
                title = "الحفيد أحمد",
                position = Offset(-90f, -15f),
                width = 100f,
                height = 70f,
                colorHex = "#2E7D32",
                parentId = "branch_left_1",
                parentAnchorId = "leaf_top",
                relativeOffset = Offset(0f, -85f)
            )

            val initialList = listOf(trunk, branchLeft, branchRight, leafLeft)
            current.copy(
                elements = syncChildrenIds(initialList)
            )
        }

        viewModelScope.launch {
            _state.collect { currentState ->
                val currentElements = currentState.elements
                if (currentState.elements != lastValidatedElements) {
                    lastValidatedElements = currentElements
                    triggerBackgroundValidation()
                }
            }
        }
    }

    /**
     * Finds which element is hit by a touch at the given canvas-space DP coordinate using 4x4 matrix inverses.
     * Searches from front to back (sorted by layerIndex).
     */
    fun findElementAt(canvasPosDp: Offset): CanvasElement? {
        val currentElements = _state.value.elements
        // Sort by layerIndex to check top layers first
        val sortedByLayer = currentElements.sortedBy { it.layerIndex }
        for (element in sortedByLayer.asReversed()) {
            if (!element.visible) continue
            
            val matrix = Matrix()
            matrix.translate(element.position.x, element.position.y, 0f)
            matrix.rotateZ(element.rotation)
            matrix.scale(element.scale, element.scale, 1f)
            
            val inverse = Matrix()
            inverse.setFrom(matrix)
            try {
                inverse.invert()
            } catch (e: Exception) {
                continue
            }
            
            val localPos = inverse.mapOffset(canvasPosDp)
            val halfW = element.width / 2f
            val halfH = element.height / 2f
            
            if (localPos.x in -halfW..halfW && localPos.y in -halfH..halfH) {
                return element
            }
        }
        return null
    }

    /**
     * Handles selection of a single element or toggles it in multi-select mode.
     */
    fun selectElement(id: String, isMultiSelect: Boolean = _state.value.isMultiSelectMode) {
        _state.update { current ->
            val newSelection = if (isMultiSelect) {
                val newSet = current.selectedElementIds.toMutableSet()
                if (newSet.contains(id)) {
                    newSet.remove(id)
                } else {
                    newSet.add(id)
                }
                newSet
            } else {
                setOf(id)
            }
            
            val updatedElements = current.elements.map { el ->
                el.copy(selected = newSelection.contains(el.id))
            }
            
            current.copy(
                selectedElementIds = newSelection,
                elements = updatedElements
            )
        }
    }

    /**
     * Clear selection.
     */
    fun deselectAll() {
        _state.update { current ->
            val updatedElements = current.elements.map { el ->
                el.copy(selected = false)
            }
            current.copy(
                selectedElementIds = emptySet(),
                elements = updatedElements
            )
        }
    }

    /**
     * Moves an element and all of its descendants recursively.
     */
    private fun moveElementAndDescendants(elementId: String, deltaDp: Offset, elements: List<CanvasElement>): List<CanvasElement> {
        val childrenIds = elements.filter { it.parentId == elementId }.map { it.id }
        var updated = elements.map { element ->
            if (element.id == elementId) {
                element.copy(position = element.position + deltaDp)
            } else {
                element
            }
        }
        for (childId in childrenIds) {
            updated = moveElementAndDescendants(childId, deltaDp, updated)
        }
        return updated
    }

    /**
     * Helper to verify if an element is a descendant of another.
     */
    private fun isDescendantOf(childId: String, potentialParentId: String, elements: List<CanvasElement>): Boolean {
        val child = elements.find { it.id == childId } ?: return false
        if (child.parentId == null) return false
        if (child.parentId == potentialParentId) return true
        return isDescendantOf(child.parentId, potentialParentId, elements)
    }

    /**
     * Drags the specified element, shifting all descendants recursively.
     * Checks if the element should detach from its parent, and scans for snapping candidates.
     */
    fun dragElement(id: String, deltaDp: Offset) {
        autoLayoutJob?.cancel()
        _state.update { current ->
            if (current.draggedElementId == null) {
                saveUndoState()
            }
            var updatedElements = current.elements
            val draggedElement = updatedElements.find { it.id == id } ?: return@update current
            
            // If the element or any ancestor is locked, prevent drag completely
            if (isLockedOrAncestorLocked(id, updatedElements)) return@update current
            
            // If it's connected, check if the drag movement breaks the connection (beyond 45dp)
            var newParentId = draggedElement.parentId
            var newParentAnchorId = draggedElement.parentAnchorId
            var newRelativeOffset = draggedElement.relativeOffset
            
            if (draggedElement.parentId != null) {
                val parent = updatedElements.find { it.id == draggedElement.parentId }
                if (parent != null) {
                    val parentAnchor = getAnchorsForType(parent.type, parent.width, parent.height)
                        .find { it.id == draggedElement.parentAnchorId }
                    if (parentAnchor != null) {
                        val absoluteParentAnchorPos = parent.position + parentAnchor.relativePos
                        val inputAnchorRel = getAnchorsForType(draggedElement.type, draggedElement.width, draggedElement.height)
                            .firstOrNull { it.isInput }?.relativePos ?: Offset.Zero
                        val newDraggedPos = draggedElement.position + deltaDp
                        val absoluteInputPos = newDraggedPos + inputAnchorRel
                        val distance = (absoluteInputPos - absoluteParentAnchorPos).getDistance()
                        
                        // Detach if dragged away
                        if (distance > 45f) {
                            newParentId = null
                            newParentAnchorId = null
                            newRelativeOffset = Offset.Zero
                        }
                    }
                }
            }
            
            // Apply detachment change to dragged element
            updatedElements = updatedElements.map { element ->
                if (element.id == id) {
                    element.copy(
                        parentId = newParentId,
                        parentAnchorId = newParentAnchorId,
                        relativeOffset = newRelativeOffset
                    )
                } else {
                    element
                }
            }
            
            // Propagate movement recursively to dragged element and all descendants
            updatedElements = moveElementAndDescendants(id, deltaDp, updatedElements)
            
            // Scan for active snapping candidates (only if the element is currently independent/detached)
            val activeDraggedNow = updatedElements.find { it.id == id }
            var snapTarget: SnapTarget? = null
            
            if (activeDraggedNow != null && activeDraggedNow.parentId == null) {
                val inputAnchorRel = getAnchorsForType(activeDraggedNow.type, activeDraggedNow.width, activeDraggedNow.height)
                    .firstOrNull { it.isInput }?.relativePos
                
                if (inputAnchorRel != null) {
                    val absoluteInputPos = activeDraggedNow.position + inputAnchorRel
                    
                    var closestDist = Float.MAX_VALUE
                    var closestParentId: String? = null
                    var closestAnchorId: String? = null
                    var closestAnchorPos: Offset? = null
                    
                    for (potParent in updatedElements) {
                        if (potParent.id == id) continue
                        // Avoid cycles
                        if (isDescendantOf(potParent.id, id, updatedElements)) continue
                        
                        val potAnchors = getAnchorsForType(potParent.type, potParent.width, potParent.height)
                            .filter { !it.isInput && it.compatibleType == activeDraggedNow.type }
                        
                        for (anchor in potAnchors) {
                            // Check if anchor is occupied
                            val isOccupied = updatedElements.any { it.parentId == potParent.id && it.parentAnchorId == anchor.id }
                            if (isOccupied) continue
                            
                            val absoluteAnchorPos = potParent.position + anchor.relativePos
                            val dist = (absoluteInputPos - absoluteAnchorPos).getDistance()
                            
                            // Snapping distance threshold is 45dp
                            if (dist < 45f && dist < closestDist) {
                                closestDist = dist
                                closestParentId = potParent.id
                                closestAnchorId = anchor.id
                                closestAnchorPos = absoluteAnchorPos
                            }
                        }
                    }
                    
                    if (closestParentId != null && closestAnchorId != null && closestAnchorPos != null) {
                        snapTarget = SnapTarget(
                            parentId = closestParentId,
                            anchorId = closestAnchorId,
                            absolutePos = closestAnchorPos
                        )
                    }
                }
            }
            
            current.copy(
                elements = updatedElements,
                draggedElementId = id,
                activeSnapTarget = snapTarget,
                isInteracting = true
            )
        }
    }

    /**
     * Adds a new design element at the specified position in canvas-space DP.
     */
    fun addElement(type: ElementType, positionDp: Offset) {
        if (type == ElementType.TRUNK) {
            val hasTrunk = _state.value.elements.any { it.type == ElementType.TRUNK }
            if (hasTrunk) {
                _state.update { it.copy(errorMessage = "يمكن لكل مشروع أن يحتوي على جذع واحد فقط.") }
                return
            }
        }

        saveUndoState()
        _state.update { current ->
            val id = "el_${java.util.UUID.randomUUID()}"
            val title = when (type) {
                ElementType.TRUNK -> "جذع عائلة جديد"
                ElementType.BRANCH -> "فرع عائلة جديد"
                ElementType.LEAF -> "ورقة جديدة"
            }
            val (width, height, color) = when (type) {
                ElementType.TRUNK -> Triple(180f, 120f, "#8B5A2B")     // Wood Brown
                ElementType.BRANCH -> Triple(150f, 100f, "#3E2723")    // Dark Wood Brown
                ElementType.LEAF -> Triple(100f, 70f, "#2E7D32")       // Foliage Green
            }
            val newElement = CanvasElement(
                id = id,
                type = type,
                title = title,
                position = positionDp,
                width = width,
                height = height,
                colorHex = color,
                selected = true // Auto-selected
            )

            val resolved = resolveOverlap(newElement, current.elements)
            if (resolved == null) {
                return@update current.copy(errorMessage = "منع التداخل: تعذر تعديل الموضع تلقائياً لمنع التراكب مع عناصر أخرى.")
            }

            // Deselect old elements
            val updatedList = current.elements.map { el -> el.copy(selected = false) } + resolved
            current.copy(
                elements = syncChildrenIds(updatedList),
                selectedElementIds = setOf(id) // Auto select the new element
            )
        }
    }

    /**
     * Deletes all currently selected elements. Detaches any child elements to prevent orphaned states.
     */
    fun deleteSelected(confirmed: Boolean = false) {
        val selectedIds = _state.value.selectedElementIds
        if (selectedIds.isEmpty()) return

        val containsTrunk = _state.value.elements.any { selectedIds.contains(it.id) && it.type == ElementType.TRUNK }
        if (containsTrunk && !confirmed) {
            _state.update { it.copy(pendingDeleteTrunkConfirmation = true) }
            return
        }

        saveUndoState()
        _state.update { current ->
            val allIdsToDelete = mutableSetOf<String>()
            selectedIds.forEach { id ->
                allIdsToDelete.add(id)
                allIdsToDelete.addAll(getAllDescendantIds(id, current.elements))
            }

            val remaining = current.elements.filter { !allIdsToDelete.contains(it.id) }
            val finalElements = syncChildrenIds(remaining).map { el ->
                el.copy(selected = false)
            }
            current.copy(
                elements = finalElements,
                selectedElementIds = emptySet(),
                pendingDeleteTrunkConfirmation = false
            )
        }
    }

    /**
     * Duplicates all currently selected elements. Detaches duplicated elements to avoid violating
     * connection multiplicity rules.
     */
    fun duplicateSelected() {
        val selectedIds = _state.value.selectedElementIds
        if (selectedIds.isEmpty()) return

        val containsTrunk = _state.value.elements.any { selectedIds.contains(it.id) && it.type == ElementType.TRUNK }
        if (containsTrunk) {
            _state.update { it.copy(errorMessage = "يمكن لكل مشروع أن يحتوي على جذع واحد فقط.") }
            return
        }

        saveUndoState()
        _state.update { current ->
            val toDuplicate = current.elements.filter { current.selectedElementIds.contains(it.id) }
            val newElements = mutableListOf<CanvasElement>()
            val newIds = mutableSetOf<String>()

            toDuplicate.forEach { original ->
                val newId = "el_dup_${java.util.UUID.randomUUID()}"
                val duplicated = original.copy(
                    id = newId,
                    position = original.position + Offset(40f, 40f),
                    parentId = null,
                    parentAnchorId = null,
                    relativeOffset = Offset.Zero,
                    selected = true
                )
                newElements.add(duplicated)
                newIds.add(newId)
            }

            // Deselect old ones
            val unselectedOld = current.elements.map { el -> el.copy(selected = false) }
            val combinedList = unselectedOld + newElements

            current.copy(
                elements = syncChildrenIds(combinedList),
                selectedElementIds = newIds
            )
        }
    }

    /**
     * Toggles multi-selection mode state.
     */
    fun toggleMultiSelectMode() {
        _state.update { current ->
            current.copy(isMultiSelectMode = !current.isMultiSelectMode)
        }
    }

    /**
     * Updates zoom scale and pan offset centered around a specific focus point (centroid).
     * This avoids content slipping under the user's fingers during pinch gestures.
     */
    fun updateTransform(zoomFactor: Float, panDelta: Offset, centroid: Offset) {
        _state.update { current ->
            val oldScale = current.scale
            // Calculate and clamp new scale
            val rawNewScale = oldScale * zoomFactor
            val newScale = rawNewScale.coerceIn(minScale, maxScale)

            // Calculate the actual factor of change after clamping
            val actualFactor = newScale / oldScale

            // Apply zoom math centered around the centroid
            val zoomedOffset = centroid - (centroid - current.offset) * actualFactor

            // Apply panning delta (which is scaled relative to current viewport zoom)
            val finalOffset = zoomedOffset + panDelta

            current.copy(
                scale = newScale,
                offset = finalOffset,
                isInteracting = true
            )
        }
    }

    /**
     * Applies simple 1-finger panning translation.
     */
    fun updatePan(panDelta: Offset) {
        _state.update { current ->
            current.copy(
                offset = current.offset + panDelta,
                isInteracting = true
            )
        }
    }

    /**
     * Updates the touch positions in screen coordinates and projects them into canvas-space coordinates.
     */
    fun updateTouchPosition(screenPos: Offset, activePointers: Int) {
        _state.update { current ->
            // Canvas Space calculation: P_canvas = (P_screen - Offset) / Scale
            val canvasX = (screenPos.x - current.offset.x) / current.scale
            val canvasY = (screenPos.y - current.offset.y) / current.scale
            val canvasPos = Offset(canvasX, canvasY)

            current.copy(
                activeTouches = activePointers,
                touchPositionScreen = screenPos,
                touchPositionCanvas = canvasPos,
                isInteracting = activePointers > 0
            )
        }
    }

    /**
     * Handles touch releases, performing connection snapping if a valid candidate is highlighted.
     */
    fun clearTouch() {
        _state.update { current ->
            val draggedId = current.draggedElementId
            val snap = current.activeSnapTarget
            
            if (draggedId != null && snap != null) {
                val dragged = current.elements.find { it.id == draggedId }
                val parent = current.elements.find { it.id == snap.parentId }
                
                if (dragged != null && parent != null) {
                    // 1. Enforce Relationship Constraints
                    if (!isValidRelation(parent.type, dragged.type)) {
                        _state.update { it.copy(errorMessage = "علاقة غير مسموحة! يسمح فقط بـ (جذع -> فرع)، (فرع -> فرع)، (فرع -> ورقة).") }
                        return@update current.copy(
                            draggedElementId = null,
                            activeSnapTarget = null,
                            activeTouches = 0,
                            isInteracting = false
                        )
                    }

                    // 2. Enforce Cycle Prevention
                    if (wouldCreateCycle(draggedId, snap.parentId, current.elements)) {
                        _state.update { it.copy(errorMessage = "لا يمكن إنشاء علاقة دائرية في شجرة العائلة.") }
                        return@update current.copy(
                            draggedElementId = null,
                            activeSnapTarget = null,
                            activeTouches = 0,
                            isInteracting = false
                        )
                    }

                    val parentAnchors = getAnchorsForType(parent.type, parent.width, parent.height)
                    val parentAnchor = parentAnchors.find { it.id == snap.anchorId }
                    
                    val inputAnchors = getAnchorsForType(dragged.type, dragged.width, dragged.height)
                    val inputAnchor = inputAnchors.find { it.isInput }
                    
                    if (parentAnchor != null && inputAnchor != null) {
                        val targetPos = parent.position + parentAnchor.relativePos - inputAnchor.relativePos
                        val displacement = targetPos - dragged.position
                        
                        // Update the child's hierarchy properties
                        var updated = current.elements.map { element ->
                            if (element.id == draggedId) {
                                element.copy(
                                    parentId = snap.parentId,
                                    parentAnchorId = snap.anchorId,
                                    relativeOffset = targetPos - parent.position
                                )
                            } else {
                                element
                            }
                        }
                        // Shift all child's descendants as well to snap them in alignment
                        updated = moveElementAndDescendants(draggedId, displacement, updated)
                        
                        return@update current.copy(
                            elements = syncChildrenIds(updated),
                            draggedElementId = null,
                            activeSnapTarget = null,
                            activeTouches = 0,
                            isInteracting = false
                        )
                    }
                }
            } else if (draggedId != null) {
                // Independent element dropped
                val dragged = current.elements.find { it.id == draggedId }
                if (dragged != null) {
                    val resolved = resolveOverlap(dragged, current.elements)
                    if (resolved == null) {
                        _state.update { it.copy(errorMessage = "تحذير: يوجد تداخل بين العناصر! يرجى نقل العناصر لتجنب التراكب يدوياً.") }
                    } else if (resolved.position != dragged.position) {
                        val displacement = resolved.position - dragged.position
                        val updated = moveElementAndDescendants(draggedId, displacement, current.elements)
                        _state.update { it.copy(errorMessage = "منع التداخل: تم تعديل موضع العنصر تلقائياً لتجنب التراكب.") }
                        return@update current.copy(
                            elements = syncChildrenIds(updated),
                            draggedElementId = null,
                            activeSnapTarget = null,
                            activeTouches = 0,
                            isInteracting = false
                        )
                    }
                }
            }
            
            current.copy(
                draggedElementId = null,
                activeSnapTarget = null,
                activeTouches = 0,
                isInteracting = false
            )
        }
    }

    /**
     * Updates a single element with a transform lambda and automatically
     * realigns any attached child elements to keep the family tree connected.
     */
    fun updateElement(id: String, transform: (CanvasElement) -> CanvasElement) {
        _state.update { current ->
            var updated = current.elements.map { element ->
                if (element.id == id) {
                    transform(element)
                } else {
                    element
                }
            }
            // Automatically realign snapped children recursively if the size of the parent changed
            updated = realignChildrenRecursively(id, updated)
            current.copy(elements = updated)
        }
    }

    /**
     * Updates an element's position from inputs/sliders, shifting all descendants recursively.
     */
    fun updateElementPosition(id: String, newPosition: Offset) {
        _state.update { current ->
            val target = current.elements.find { it.id == id } ?: return@update current
            val delta = newPosition - target.position
            val updated = moveElementAndDescendants(id, delta, current.elements)
            current.copy(elements = updated)
        }
    }

    /**
     * Shows or hides the professional properties sidebar.
     */
    fun showPropertiesSidebar(id: String?) {
        _state.update { current ->
            current.copy(activePropertiesElementId = id)
        }
    }

    /**
     * Automatically adjusts child elements' positions to keep them snapped to the parent's anchors.
     */
    private fun realignChildrenRecursively(parentId: String, elements: List<CanvasElement>): List<CanvasElement> {
        val parent = elements.find { it.id == parentId } ?: return elements
        var updated = elements
        val children = elements.filter { it.parentId == parentId }
        for (child in children) {
            val parentAnchors = getAnchorsForType(parent.type, parent.width, parent.height)
            val parentAnchor = parentAnchors.find { it.id == child.parentAnchorId }
            val childInputAnchors = getAnchorsForType(child.type, child.width, child.height)
            val childInputAnchor = childInputAnchors.find { it.isInput }
            
            if (parentAnchor != null && childInputAnchor != null) {
                val targetPos = parent.position + parentAnchor.relativePos - childInputAnchor.relativePos
                val displacement = targetPos - child.position
                if (displacement != Offset.Zero) {
                    updated = updated.map { el ->
                        if (el.id == child.id) {
                            el.copy(
                                position = targetPos,
                                relativeOffset = targetPos - parent.position
                            )
                        } else {
                            el
                        }
                    }
                    // Recursively realign grandchildren
                    updated = realignChildrenRecursively(child.id, updated)
                }
            }
        }
        return updated
    }

    /**
     * Zooms in step-wise, centered around the viewport center or a given point.
     */
    fun zoomIn(center: Offset) {
        updateTransform(1.2f, Offset.Zero, center)
    }

    /**
     * Zooms out step-wise, centered around the viewport center or a given point.
     */
    fun zoomOut(center: Offset) {
        updateTransform(0.8f, Offset.Zero, center)
    }

    /**
     * Resets the viewport transformation back to default 100% scale and center.
     */
    fun resetCanvas() {
        _state.update { current ->
            current.copy(
                scale = 1.0f,
                offset = Offset.Zero,
                activeTouches = 0,
                touchPositionScreen = Offset.Zero,
                touchPositionCanvas = Offset.Zero,
                isInteracting = false
            )
        }
    }

    fun setErrorMessage(msg: String?) {
        _state.update { it.copy(errorMessage = msg) }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun cancelDeleteTrunk() {
        _state.update { it.copy(pendingDeleteTrunkConfirmation = false) }
    }

    fun isValidRelation(parentType: ElementType, childType: ElementType): Boolean {
        return when {
            parentType == ElementType.TRUNK && childType == ElementType.BRANCH -> true
            parentType == ElementType.BRANCH && childType == ElementType.BRANCH -> true
            parentType == ElementType.BRANCH && childType == ElementType.LEAF -> true
            else -> false
        }
    }

    fun wouldCreateCycle(childId: String, parentId: String, elements: List<CanvasElement>): Boolean {
        if (childId == parentId) return true
        var currentParentId: String? = parentId
        while (currentParentId != null) {
            if (currentParentId == childId) return true
            currentParentId = elements.find { it.id == currentParentId }?.parentId
        }
        return false
    }

    private fun getAllDescendantIds(elementId: String, elements: List<CanvasElement>): Set<String> {
        val descendants = mutableSetOf<String>()
        fun traverse(id: String) {
            elements.filter { it.parentId == id }.forEach { child ->
                if (!descendants.contains(child.id)) {
                    descendants.add(child.id)
                    traverse(child.id)
                }
            }
        }
        traverse(elementId)
        return descendants
    }

    fun elementsOverlap(el1: CanvasElement, el2: CanvasElement): Boolean {
        val r1Left = el1.position.x - el1.width / 2f
        val r1Right = el1.position.x + el1.width / 2f
        val r1Top = el1.position.y - el1.height / 2f
        val r1Bottom = el1.position.y + el1.height / 2f

        val r2Left = el2.position.x - el2.width / 2f
        val r2Right = el2.position.x + el2.width / 2f
        val r2Top = el2.position.y - el2.height / 2f
        val r2Bottom = el2.position.y + el2.height / 2f

        return !(r1Right < r2Left || r1Left > r2Right || r1Bottom < r2Top || r1Top > r2Bottom)
    }

    fun resolveOverlap(element: CanvasElement, allElements: List<CanvasElement>): CanvasElement? {
        var currentElement = element
        // Direct parent/child pairs are MEANT to sit close together (they're visually
        // joined by a connecting branch line via anchors), so they must never be treated
        // as an "overlap" to push apart. Only unrelated elements should repel each other.
        val otherElements = allElements.filter {
            it.id != element.id &&
            it.id != element.parentId &&
            it.parentId != element.id
        }
        
        var attempts = 0
        val maxAttempts = 8
        val offsetStep = Offset(30f, 30f)
        
        while (attempts < maxAttempts) {
            val overlapping = otherElements.any { elementsOverlap(currentElement, it) }
            if (!overlapping) {
                return currentElement
            }
            currentElement = currentElement.copy(position = currentElement.position + offsetStep)
            attempts++
        }
        return null
    }

    private var validationJob: Job? = null
    private var lastValidatedElements: List<CanvasElement>? = null

    private fun triggerBackgroundValidation() {
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            delay(1000)
            val elementsToValidate = _state.value.elements
            val repairedElements = withContext(Dispatchers.Default) {
                validateAndRepair(elementsToValidate)
            }
            if (repairedElements != elementsToValidate) {
                lastValidatedElements = repairedElements
                _state.update { current ->
                    current.copy(
                        elements = repairedElements,
                        validationStatus = "تم التحقق من سلامة شجرة العائلة وتصحيح الأخطاء الممكنة تلقائياً."
                    )
                }
            } else {
                _state.update { current ->
                    current.copy(
                        validationStatus = "شجرة العائلة سليمة ومتوافقة مع جميع القيود."
                    )
                }
            }
        }
    }

    private fun validateAndRepair(elements: List<CanvasElement>): List<CanvasElement> {
        var list = elements.toMutableList()
        var changed = false

        // 1. UUID uniqueness
        val seenIds = mutableSetOf<String>()
        val duplicatesMap = mutableMapOf<String, String>()
        for (i in list.indices) {
            val el = list[i]
            if (seenIds.contains(el.id)) {
                val newId = "el_${java.util.UUID.randomUUID()}"
                duplicatesMap[el.id] = newId
                list[i] = el.copy(id = newId)
                changed = true
            } else {
                seenIds.add(el.id)
            }
        }
        if (duplicatesMap.isNotEmpty()) {
            for (i in list.indices) {
                val el = list[i]
                if (el.parentId != null && duplicatesMap.containsKey(el.parentId)) {
                    list[i] = el.copy(parentId = duplicatesMap[el.parentId])
                    changed = true
                }
            }
        }

        // 2. Orphaned elements check
        for (i in list.indices) {
            val el = list[i]
            if (el.parentId != null) {
                val parentExists = list.any { it.id == el.parentId }
                if (!parentExists) {
                    list[i] = el.copy(parentId = null, parentAnchorId = null, relativeOffset = Offset.Zero)
                    changed = true
                }
            }
        }

        // 3. Relationship rules & cycle prevention
        for (i in list.indices) {
            val el = list[i]
            if (el.parentId != null) {
                val parent = list.find { it.id == el.parentId }
                if (parent != null) {
                    val isValid = isValidRelation(parent.type, el.type)
                    val isCycle = wouldCreateCycle(el.id, parent.id, list)
                    if (!isValid || isCycle) {
                        list[i] = el.copy(parentId = null, parentAnchorId = null, relativeOffset = Offset.Zero)
                        changed = true
                    }
                }
            }
        }

        // 4. Trunk Count rule (Only 1 Trunk allowed per project)
        val trunkElements = list.filter { it.type == ElementType.TRUNK }
        if (trunkElements.size > 1) {
            var trunkKept = false
            for (i in list.indices) {
                val el = list[i]
                if (el.type == ElementType.TRUNK) {
                    if (!trunkKept) {
                        trunkKept = true
                    } else {
                        list[i] = el.copy(
                            type = ElementType.BRANCH,
                            title = el.title.replace("جذع", "فرع")
                        )
                        changed = true
                    }
                }
            }
        }

        // 5. Trunk names limits
        for (i in list.indices) {
            val el = list[i]
            if (el.type == ElementType.TRUNK) {
                val trimmedNames = el.trunkNames.toMutableList()
                if (trimmedNames.size > 10) {
                    list[i] = el.copy(trunkNames = trimmedNames.take(10))
                    changed = true
                }
            }
        }

        if (changed) {
            list = syncChildrenIds(list).toMutableList()
        }
        return list
    }

    // Phase 9: Smart Layout Engine

    private var autoLayoutJob: Job? = null

    fun selectLayoutMode(mode: String) {
        _state.update { it.copy(selectedLayoutMode = mode) }
    }

    private class TreeLayoutNode(val element: CanvasElement) {
        val children = mutableListOf<TreeLayoutNode>()
    }

    fun triggerAutoLayout() {
        autoLayoutJob?.cancel()
        saveUndoState()
        
        val elements = _state.value.elements
        if (elements.isEmpty()) return

        // 1. Build tree structure
        val idToNode = elements.associate { it.id to TreeLayoutNode(it) }
        val roots = mutableListOf<TreeLayoutNode>()
        for (element in elements) {
            val node = idToNode[element.id] ?: continue
            val parentId = element.parentId
            if (parentId != null && idToNode.containsKey(parentId)) {
                idToNode[parentId]?.children?.add(node)
            } else {
                roots.add(node)
            }
        }

        // 2. Find Trunk as the primary root reference
        val trunkNode = roots.find { it.element.type == ElementType.TRUNK } ?: roots.firstOrNull() ?: return
        val trunkPos = trunkNode.element.position // Trunk remains at center/current position

        // 3. Compute target positions based on layout style
        val targetPositions = mutableMapOf<String, Offset>()
        targetPositions[trunkNode.element.id] = trunkPos

        when (_state.value.selectedLayoutMode) {
            "VERTICAL" -> calculateVerticalLayout(trunkNode, trunkPos, targetPositions)
            "HORIZONTAL" -> calculateHorizontalLayout(trunkNode, trunkPos, targetPositions)
            "CIRCULAR" -> calculateCircularLayout(trunkNode, trunkPos, targetPositions)
            else -> calculateFreeLayout(trunkNode, trunkPos, targetPositions) // FREE
        }

        // Layout any other orphaned roots relative to the trunk
        val otherRoots = roots.filter { it.element.id != trunkNode.element.id }
        otherRoots.forEachIndexed { idx, root ->
            val offset = when (_state.value.selectedLayoutMode) {
                "VERTICAL" -> Offset((idx + 1) * 300f, 0f)
                "HORIZONTAL" -> Offset(0f, (idx + 1) * 250f)
                "CIRCULAR" -> Offset((idx + 1) * 350f, (idx + 1) * 350f)
                else -> Offset((idx + 1) * 300f, 0f)
            }
            val rootPos = trunkPos + offset
            targetPositions[root.element.id] = rootPos
            when (_state.value.selectedLayoutMode) {
                "VERTICAL" -> calculateVerticalLayout(root, rootPos, targetPositions)
                "HORIZONTAL" -> calculateHorizontalLayout(root, rootPos, targetPositions)
                "CIRCULAR" -> calculateCircularLayout(root, rootPos, targetPositions)
                else -> calculateFreeLayout(root, rootPos, targetPositions)
            }
        }

        // 4. Post-processing Overlap Resolution Pass
        val elementsWithTargets = elements.map { el ->
            val target = targetPositions[el.id]
            if (target != null) el.copy(position = target) else el
        }
        val resolvedElements = resolveAllOverlapsInternal(elementsWithTargets)
        val finalTargetPositions = resolvedElements.associate { it.id to it.position }

        // 5. Run smooth animation
        animateElementsTo(finalTargetPositions)
    }

    private fun calculateVerticalLayout(parent: TreeLayoutNode, parentPos: Offset, targets: MutableMap<String, Offset>) {
        val children = parent.children
        if (children.isEmpty()) return

        // Separate branches and leaves to layout nicely
        val branches = children.filter { it.element.type != ElementType.LEAF }
        val leaves = children.filter { it.element.type == ElementType.LEAF }

        // 1. Layout branches horizontally spread
        if (branches.isNotEmpty()) {
            val totalWidthNeeded = calculateHorizontalWidthNeeded(branches, spacingMargin = 40f)
            var currentX = parentPos.x - totalWidthNeeded / 2f
            
            branches.forEachIndexed { idx, child ->
                if (idx > 0) {
                    val prev = branches[idx - 1]
                    currentX += (prev.element.width + child.element.width) / 2f + 40f
                } else {
                    currentX += child.element.width / 2f
                }
                
                val stepY = -((parent.element.height + child.element.height) / 2f + 70f)
                val childPos = Offset(currentX, parentPos.y + stepY)
                targets[child.element.id] = childPos
                calculateVerticalLayout(child, childPos, targets)
            }
        }

        // 2. Layout leaves clustered at parent top
        if (leaves.isNotEmpty()) {
            val leafWidthNeeded = calculateHorizontalWidthNeeded(leaves, spacingMargin = 20f)
            var currentX = parentPos.x - leafWidthNeeded / 2f
            
            leaves.forEachIndexed { idx, child ->
                if (idx > 0) {
                    val prev = leaves[idx - 1]
                    currentX += (prev.element.width + child.element.width) / 2f + 20f
                } else {
                    currentX += child.element.width / 2f
                }
                val stepY = -((parent.element.height + child.element.height) / 2f + 50f)
                val childPos = Offset(currentX, parentPos.y + stepY)
                targets[child.element.id] = childPos
            }
        }
    }

    private fun calculateHorizontalWidthNeeded(nodes: List<TreeLayoutNode>, spacingMargin: Float): Float {
        if (nodes.isEmpty()) return 0f
        var total = 0f
        for (i in nodes.indices) {
            total += nodes[i].element.width
            if (i > 0) {
                total += spacingMargin
            }
        }
        return total
    }

    private fun calculateHorizontalLayout(parent: TreeLayoutNode, parentPos: Offset, targets: MutableMap<String, Offset>) {
        val children = parent.children
        if (children.isEmpty()) return

        if (parent.element.type == ElementType.TRUNK) {
            val leftGroup = children.filterIndexed { idx, _ -> idx % 2 == 0 }
            val rightGroup = children.filterIndexed { idx, _ -> idx % 2 != 0 }
            calculateHorizontalSidedLayout(leftGroup, parentPos, sideSign = -1f, targets)
            calculateHorizontalSidedLayout(rightGroup, parentPos, sideSign = 1f, targets)
        } else {
            calculateHorizontalSidedLayout(children, parentPos, sideSign = if (parentPos.x < 0) -1f else 1f, targets)
        }
    }

    private fun calculateHorizontalSidedLayout(nodes: List<TreeLayoutNode>, parentPos: Offset, sideSign: Float, targets: MutableMap<String, Offset>) {
        if (nodes.isEmpty()) return

        var totalHeight = 0f
        for (i in nodes.indices) {
            totalHeight += nodes[i].element.height
            if (i > 0) totalHeight += 30f
        }

        var currentY = parentPos.y - totalHeight / 2f

        nodes.forEachIndexed { idx, child ->
            if (idx > 0) {
                val prev = nodes[idx - 1]
                currentY += (prev.element.height + child.element.height) / 2f + 30f
            } else {
                currentY += child.element.height / 2f
            }

            val stepX = ((child.element.width + child.element.width) / 2f + 110f) * sideSign
            val childPos = Offset(parentPos.x + stepX, currentY)
            targets[child.element.id] = childPos

            calculateHorizontalSidedLayout(child.children, childPos, sideSign, targets)
        }
    }

    private fun calculateCircularLayout(parent: TreeLayoutNode, parentPos: Offset, targets: MutableMap<String, Offset>, parentAngle: Double = 0.0, radius: Float = 0f, level: Int = 1) {
        val children = parent.children
        if (children.isEmpty()) return

        if (parent.element.type == ElementType.TRUNK) {
            val angleStep = (2 * Math.PI) / children.size
            children.forEachIndexed { index, child ->
                val angle = index * angleStep
                val stepRadius = (parent.element.height + child.element.height) / 2f + 120f
                val childX = parentPos.x + stepRadius * kotlin.math.cos(angle).toFloat()
                val childY = parentPos.y + stepRadius * kotlin.math.sin(angle).toFloat()
                val childPos = Offset(childX, childY)
                targets[child.element.id] = childPos
                calculateCircularLayout(child, childPos, targets, angle, stepRadius, level + 1)
            }
        } else {
            val stepRadius = (parent.element.height + parent.element.height) / 2f + 100f
            val spreadAngle = Math.PI / 3.0 // 60 degrees spread
            val startAngle = parentAngle - spreadAngle / 2f
            val angleStep = if (children.size > 1) spreadAngle / (children.size - 1) else 0.0

            children.forEachIndexed { index, child ->
                val angle = if (children.size > 1) startAngle + index * angleStep else parentAngle
                val childX = parentPos.x + stepRadius * kotlin.math.cos(angle).toFloat()
                val childY = parentPos.y + stepRadius * kotlin.math.sin(angle).toFloat()
                val childPos = Offset(childX, childY)
                targets[child.element.id] = childPos
                calculateCircularLayout(child, childPos, targets, angle, radius + stepRadius, level + 1)
            }
        }
    }

    private fun calculateFreeLayout(parent: TreeLayoutNode, parentPos: Offset, targets: MutableMap<String, Offset>, parentAngle: Double = -Math.PI / 2, radius: Float = 0f, level: Int = 1) {
        val children = parent.children
        if (children.isEmpty()) return

        if (parent.element.type == ElementType.TRUNK) {
            val minAngle = -135.0 * Math.PI / 180.0
            val maxAngle = -45.0 * Math.PI / 180.0
            val range = maxAngle - minAngle
            val angleStep = if (children.size > 1) range / (children.size - 1) else 0.0
            val startAngle = if (children.size > 1) minAngle else -Math.PI / 2

            children.forEachIndexed { index, child ->
                val angle = startAngle + index * angleStep
                val stepRadius = (parent.element.height + child.element.height) / 2f + 130f
                val childX = parentPos.x + stepRadius * kotlin.math.cos(angle).toFloat()
                val childY = parentPos.y + stepRadius * kotlin.math.sin(angle).toFloat()
                val childPos = Offset(childX, childY)
                targets[child.element.id] = childPos
                calculateFreeLayout(child, childPos, targets, angle, stepRadius, level + 1)
            }
        } else {
            val stepRadius = (parent.element.height + parent.element.height) / 2f + 100f
            val spreadAngle = 55.0 * Math.PI / 180.0
            val startAngle = parentAngle - spreadAngle / 2f
            val angleStep = if (children.size > 1) spreadAngle / (children.size - 1) else 0.0

            children.forEachIndexed { index, child ->
                val angle = if (children.size > 1) startAngle + index * angleStep else parentAngle
                val childX = parentPos.x + stepRadius * kotlin.math.cos(angle).toFloat()
                val childY = parentPos.y + stepRadius * kotlin.math.sin(angle).toFloat()
                val childPos = Offset(childX, childY)
                targets[child.element.id] = childPos
                calculateFreeLayout(child, childPos, targets, angle, radius + stepRadius, level + 1)
            }
        }
    }

    private fun resolveAllOverlapsInternal(elements: List<CanvasElement>, iterations: Int = 8): List<CanvasElement> {
        val list = elements.map { it.copy() }.toMutableList()
        val minSpacing = 24f // Safety spacing in DP

        for (iter in 1..iterations) {
            var shiftedAny = false
            for (i in list.indices) {
                for (j in list.indices) {
                    if (i == j) continue
                    val el1 = list[i]
                    val el2 = list[j]

                    // Same as above: never repel a direct parent from its own child.
                    // They're supposed to sit close together, connected by a branch line.
                    if (el1.parentId == el2.id || el2.parentId == el1.id) continue

                    val r1Left = el1.position.x - el1.width / 2f
                    val r1Right = el1.position.x + el1.width / 2f
                    val r1Top = el1.position.y - el1.height / 2f
                    val r1Bottom = el1.position.y + el1.height / 2f

                    val r2Left = el2.position.x - el2.width / 2f
                    val r2Right = el2.position.x + el2.width / 2f
                    val r2Top = el2.position.y - el2.height / 2f
                    val r2Bottom = el2.position.y + el2.height / 2f

                    val overlapX = (r1Right + minSpacing > r2Left) && (r1Left - minSpacing < r2Right)
                    val overlapY = (r1Bottom + minSpacing > r2Top) && (r1Top - minSpacing < r2Bottom)

                    if (overlapX && overlapY) {
                        val delta = el1.position - el2.position
                        val dist = delta.getDistance()
                        val pushDir = if (dist > 0.1f) delta / dist else Offset(1f, 0.1f)

                        val pushAmount = 35f
                        if (el1.type != ElementType.TRUNK) {
                            list[i] = el1.copy(position = el1.position + pushDir * (pushAmount / 2f))
                        }
                        if (el2.type != ElementType.TRUNK) {
                            list[j] = el2.copy(position = el2.position - pushDir * (pushAmount / 2f))
                        }
                        shiftedAny = true
                    }
                }
            }
            if (!shiftedAny) break
        }
        return list
    }

    private fun animateElementsTo(targetPositions: Map<String, Offset>) {
        autoLayoutJob = viewModelScope.launch {
            val durationMs = 450L
            val steps = 25L
            val stepDuration = durationMs / steps
            val startPositions = _state.value.elements.associate { it.id to it.position }

            for (step in 1..steps) {
                delay(stepDuration)
                val fraction = step.toFloat() / steps
                val t = fraction * fraction * (3 - 2 * fraction)

                _state.update { current ->
                    val animatedElements = current.elements.map { element ->
                        val start = startPositions[element.id]
                        val target = targetPositions[element.id]
                        if (start != null && target != null) {
                            element.copy(position = start + (target - start) * t)
                        } else {
                            element
                        }
                    }
                    val aligned = realignAllOffsets(animatedElements)
                    current.copy(elements = syncChildrenIds(aligned))
                }
            }
        }
    }

    fun autoAssignAnchor(parent: CanvasElement, child: CanvasElement): String {
        val dx = child.position.x - parent.position.x
        val dy = child.position.y - parent.position.y
        return when (parent.type) {
            ElementType.TRUNK -> {
                if (dx < 0) {
                    if (dy < 0) "left_1" else "left_2"
                } else {
                    if (dy < 0) "right_1" else "right_2"
                }
            }
            ElementType.BRANCH -> {
                if (child.type == ElementType.LEAF) {
                    "leaf_top"
                } else {
                    if (dx < 0) "branch_left" else "branch_right"
                }
            }
            ElementType.LEAF -> "input"
        }
    }

    fun realignAllOffsets(elements: List<CanvasElement>): List<CanvasElement> {
        return elements.map { element ->
            val parentId = element.parentId
            if (parentId != null) {
                val parent = elements.find { it.id == parentId }
                if (parent != null) {
                    val anchorId = autoAssignAnchor(parent, element)
                    val parentAnchors = getAnchorsForType(parent.type, parent.width, parent.height)
                    val parentAnchor = parentAnchors.find { it.id == anchorId }
                    
                    val inputAnchors = getAnchorsForType(element.type, element.width, element.height)
                    val inputAnchor = inputAnchors.find { it.isInput }
                    
                    if (parentAnchor != null && inputAnchor != null) {
                        element.copy(
                            parentAnchorId = anchorId,
                            relativeOffset = element.position - parent.position
                        )
                    } else {
                        element
                    }
                } else {
                    element
                }
            } else {
                element
            }
        }
    }
}
