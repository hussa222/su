package com.example.canvas.engine

import androidx.compose.ui.geometry.Offset
import com.example.models.CanvasElement

object TreeMover {

    fun moveTree(

        id:String,

        delta:Offset,

        elements:MutableList<CanvasElement>

    ){

        val current=

            elements.find{

                it.id==id

            }?:return

        current.position+=delta

        val children=

            elements.filter{

                it.parentId==id

            }

        children.forEach{

            moveTree(

                it.id,

                delta,

                elements

            )

        }

    }

}