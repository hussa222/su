package com.example.models

import androidx.compose.ui.geometry.Offset

/**
 * Types of elements that can be placed on the canvas for the Family Tree.
 */
enum class ElementType {
    TRUNK,     // الجذع
    BRANCH,    // الفرع
    LEAF       // الورقة
}

/**
 * Strength of the automatic overlap-prevention system.
 * STRONG: elements are pushed apart the instant their bounding boxes touch (original behavior).
 * LIGHT: elements are allowed to overlap partially before being pushed apart.
 * OFF: overlap prevention is disabled entirely; elements can overlap freely.
 */
enum class OverlapMode {
    OFF,
    LIGHT,
    STRONG
}

/**
 * Represents a design element in canvas-space.
 * Positions are defined in canvas-space DP coordinates.
 */
data class CanvasElement(
    val id: String,
    val type: ElementType,
    val title: String,
    val position: Offset,         // Center position in canvas-space DP
    val width: Float,              // Width in DP
    val height: Float,             // Height in DP
    val extraData: String = "",
    val colorHex: String = "#8B5A2B", // Default wood brown
    
    // Phase 4: Family Tree Relations
    val parentId: String? = null,
    val parentAnchorId: String? = null, // The anchor ID on the parent element this is connected to
    val relativeOffset: Offset = Offset.Zero, // Position relative to parent's center when snapped

    // Phase 5: Transform Properties
    val rotation: Float = 0f,              // Rotation angle in degrees (0 to 360)
    val curvature: Float = 0f,             // Curvature sweep (-100 to 100)
    val thickness: Float = 8f,             // Core thickness of the trunk/branch lines in DP
    val opacity: Float = 1.0f,             // Element opacity (0.0 to 1.0)
    val borderColorHex: String = "#FFFFFF",// Border color in hex
    val borderThickness: Float = 2.0f,     // Border thickness in DP
    val hasShadow: Boolean = true,         // Enable shadow
    val shadowElevation: Float = 4.0f,     // Shadow elevation in DP
    val scale: Float = 1.0f,               // Scaling factor
    val locked: Boolean = false,           // Lock element from movement
    val visible: Boolean = true,           // Visibility toggle
    val selected: Boolean = false,         // Selection toggle state
    val layerIndex: Int = 0,               // Render layer ordering index
    val childrenIds: List<String> = emptyList(), // Connected child element IDs

    // Text & Style Properties
    val fontSize: Float = 12f,
    val fontColorHex: String = "#FFFFFF",
    val fontFamily: String = "SANS_SERIF", // SANS_SERIF, SERIF, MONOSPACE, DEFAULT
    val alignment: String = "Center",      // Left, Center, Right
    
    // Trunk specific: supports up to 10 names
    val trunkNames: List<String> = emptyList(),
    val trunkItemSpacing: Float = 6f,      // Spacing between names in DP

    // Branch specific: Circle head (دائرة رأس الفرع) settings
    val branchCircleDiameter: Float = 36f,
    val branchCircleColorHex: String = "#81C784",
    val branchCircleBorderColorHex: String = "#FFFFFF",
    val branchCircleBorderThickness: Float = 1.5f,
    val branchCircleTextColorHex: String = "#FFFFFF",
    val branchCircleTextSize: Float = 10f,
    val branchCircleFontFamily: String = "SANS_SERIF",

    // Leaf specific: text position inside the leaf
    val leafTextPosition: String = "Center" // Center, Top, Bottom, Left, Right
)

/**
 * Represents a highlighted connection snapping target during drag.
 */
data class SnapTarget(
    val parentId: String,
    val anchorId: String,
    val absolutePos: Offset // Absolute position of the parent's anchor in canvas-space DP
)

/**
 * Represents the state of the drawing canvas.
 * Includes zoom scale, panning offset, touch interaction, and coordinate tracking.
 */
data class CanvasState(
    val scale: Float = 1.0f,
    val offset: Offset = Offset.Zero,
    val activeTouches: Int = 0,
    val touchPositionScreen: Offset = Offset.Zero,
    val touchPositionCanvas: Offset = Offset.Zero,
    val isInteracting: Boolean = false,
    
    // Phase 3 & 4: Interactive Elements State
    val elements: List<CanvasElement> = emptyList(),
    val selectedElementIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val draggedElementId: String? = null,
    
    // Active connection snapping target during drag
    val activeSnapTarget: SnapTarget? = null,

    // Phase 5 Properties Sidebar
    val activePropertiesElementId: String? = null, // ID of the element whose properties are open in sidebar
    
    // Phase 6 Undo / Redo Reactive States
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    // Phase 8: Smart Rules Engine
    val errorMessage: String? = null,
    val pendingDeleteTrunkConfirmation: Boolean = false,
    val validationStatus: String? = null,

    // Phase 9: Smart Layout Engine
    val selectedLayoutMode: String = "FREE", // FREE, VERTICAL, HORIZONTAL, CIRCULAR

    // Phase 10: Adjustable Overlap Prevention Strength
    val overlapMode: OverlapMode = OverlapMode.LIGHT
)
