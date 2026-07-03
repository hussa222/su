package com.example.providers

import androidx.compose.ui.geometry.Offset
import com.example.models.CanvasElement
import com.example.models.ElementType
import kotlin.math.sqrt

data class Anchor(

    val id: String,

    val relativePos: Offset,

    val isInput: Boolean = false,

    val maxChildren: Int = 1,

    val childrenIds: MutableList<String> = mutableListOf()

)

data class SnapTarget(

    val parentId: String,

    val anchorId: String,

    val worldPosition: Offset,

    val distance: Float

)

object AnchorManager {

    const val SNAP_DISTANCE = 24f

    fun getAnchors(

        element: CanvasElement

    ): List<Anchor> {

        return when (element.type) {

            ElementType.TRUNK -> trunkAnchors(element)

            ElementType.BRANCH -> branchAnchors(element)

            ElementType.LEAF -> leafAnchors(element)

        }

    }

    private fun trunkAnchors(

        element: CanvasElement

    ): List<Anchor> {

        val w = element.width

        val h = element.height

        return listOf(

            Anchor(

                id = "left_1",

                relativePos = Offset(

                    -w / 2,

                    -h * 0.25f

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "left_2",

                relativePos = Offset(

                    -w / 2,

                    h * 0.20f

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "right_1",

                relativePos = Offset(

                    w / 2,

                    -h * 0.25f

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "right_2",

                relativePos = Offset(

                    w / 2,

                    h * 0.20f

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "top_1",

                relativePos = Offset(

                    -w * 0.15f,

                    -h / 2

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "top_2",

                relativePos = Offset(

                    w * 0.15f,

                    -h / 2

                ),

                maxChildren = 1

            )

        )

    }

    private fun branchAnchors(

        element: CanvasElement

    ): List<Anchor> {

        val w = element.width

        val h = element.height

        return listOf(

            Anchor(

                id = "input",

                relativePos = Offset(

                    0f,

                    h / 2

                ),

                isInput = true

            ),

            Anchor(

                id = "left",

                relativePos = Offset(

                    -w * 0.30f,

                    -h * 0.20f

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "right",

                relativePos = Offset(

                    w * 0.30f,

                    -h * 0.20f

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "top",

                relativePos = Offset(

                    0f,

                    -h / 2

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "leaf_left",

                relativePos = Offset(

                    -w * 0.18f,

                    -h * 0.55f

                ),

                maxChildren = 1

            ),

            Anchor(

                id = "leaf_right",

                relativePos = Offset(

                    w * 0.18f,

                    -h * 0.55f

                ),

                maxChildren = 1

            )

        )

    }

    private fun leafAnchors(

        element: CanvasElement

    ): List<Anchor> {

        val h = element.height

        return listOf(

            Anchor(

                id = "input",

                relativePos = Offset(

                    0f,

                    h / 2

                ),

                isInput = true

            )

        )

    }    fun getInputAnchor(
        element: CanvasElement
    ): Anchor? {

        return getAnchors(element).firstOrNull {
            it.isInput
        }

    }

    fun getOutputAnchors(
        element: CanvasElement
    ): List<Anchor> {

        return getAnchors(element).filter {
            !it.isInput
        }

    }

    fun canAttach(
        parent: CanvasElement,
        child: CanvasElement
    ): Boolean {

        return when (child.type) {

            ElementType.TRUNK -> false

            ElementType.BRANCH -> {

                parent.type == ElementType.TRUNK ||
                parent.type == ElementType.BRANCH

            }

            ElementType.LEAF -> {

                parent.type == ElementType.BRANCH

            }

        }

    }

    fun isValidConnection(
        parent: CanvasElement,
        child: CanvasElement
    ): Boolean {

        if (parent.id == child.id)
            return false

        if (!canAttach(parent, child))
            return false

        return true

    }

    fun canAcceptMoreChildren(
        anchor: Anchor
    ): Boolean {

        return anchor.childrenIds.size < anchor.maxChildren

    }

    fun findNearestAnchor(

        dragged: CanvasElement,

        allElements: List<CanvasElement>

    ): SnapTarget? {

        var nearest: SnapTarget? = null

        var nearestDistance = Float.MAX_VALUE

        val input = getInputAnchor(dragged)
            ?: return null

        val draggedWorld =

            dragged.position + input.relativePos

        allElements.forEach { parent ->

            if (parent.id == dragged.id)
                return@forEach

            if (!canAttach(parent, dragged))
                return@forEach

            getOutputAnchors(parent)

                .forEach { anchor ->

                    if (!canAcceptMoreChildren(anchor))
                        return@forEach

                    val world =

                        parent.position +
                        anchor.relativePos

                    val dx =
                        world.x - draggedWorld.x

                    val dy =
                        world.y - draggedWorld.y

                    val distance =
                        sqrt(dx * dx + dy * dy)

                    if (

                        distance < SNAP_DISTANCE &&
                        distance < nearestDistance

                    ) {

                        nearestDistance = distance

                        nearest = SnapTarget(

                            parentId = parent.id,

                            anchorId = anchor.id,

                            worldPosition = world,

                            distance = distance

                        )

                    }

                }

        }

        return nearest

    }

    fun snapPosition(

        dragged: CanvasElement,

        target: SnapTarget

    ): Offset {

        val input =
            getInputAnchor(dragged)
                ?: return dragged.position

        return Offset(

            target.worldPosition.x -
                    input.relativePos.x,

            target.worldPosition.y -
                    input.relativePos.y

        )

    }    fun attachElement(
        parent: CanvasElement,
        child: CanvasElement,
        anchorId: String
    ): CanvasElement {

        child.parentId = parent.id
        child.parentAnchorId = anchorId

        return child
    }

    fun detachElement(
        child: CanvasElement
    ): CanvasElement {

        child.parentId = null
        child.parentAnchorId = null

        return child
    }

    fun removeConnections(
        element: CanvasElement,
        allElements: MutableList<CanvasElement>
    ) {

        allElements.forEach {

            if (it.parentId == element.id) {

                it.parentId = null
                it.parentAnchorId = null

            }

        }

    }

    fun updateChildrenPositions(
        parent: CanvasElement,
        elements: MutableList<CanvasElement>
    ) {

        val children = elements.filter {

            it.parentId == parent.id

        }

        children.forEach { child ->

            val target = findNearestAnchor(
                child,
                listOf(parent)
            )

            if (target != null) {

                child.position =
                    snapPosition(child, target)

            }

            updateChildrenPositions(
                child,
                elements
            )

        }

    }
