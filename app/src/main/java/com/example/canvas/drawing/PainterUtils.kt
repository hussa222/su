package com.example.canvas.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object PainterUtils {

    fun woodGradient(): Brush {

        return Brush.verticalGradient(

            listOf(

                Color(0xffA06A3B),

                Color(0xff8A5A31),

                Color(0xff704322),

                Color(0xff563218)

            )

        )

    }

    fun leafGradient(): Brush {

        return Brush.verticalGradient(

            listOf(

                Color(0xff9CCC65),

                Color(0xff66BB6A),

                Color(0xff388E3C),

                Color(0xff1B5E20)

            )

        )

    }

    fun branchGradient(): Brush {

        return Brush.verticalGradient(

            listOf(

                Color(0xff8D6E63),

                Color(0xff795548),

                Color(0xff5D4037)

            )

        )

    }

    fun randomKnots(
        width: Float,
        height: Float,
        count: Int
    ): List<Offset> {

        val list = mutableListOf<Offset>()

        repeat(count){

            list.add(

                Offset(

                    Random.nextFloat()*width,

                    Random.nextFloat()*height

                )

            )

        }

        return list

    }

    fun crackPath(

        start: Offset,

        length: Float

    ): Path{

        val p=Path()

        p.moveTo(start.x,start.y)

        p.lineTo(

            start.x+length*0.2f,

            start.y+length*0.15f

        )

        p.lineTo(

            start.x-length*0.1f,

            start.y+length*0.35f

        )

        p.lineTo(

            start.x+length*0.15f,

            start.y+length*0.60f

        )

        p.lineTo(

            start.x,

            start.y+length

        )

        return p

    }

    fun circlePoints(

        center:Offset,

        radius:Float,

        points:Int

    ):List<Offset>{

        val list= mutableListOf<Offset>()

        repeat(points){

            val angle=Math.toRadians(

                (360.0/points)*it

            )

            list.add(

                Offset(

                    center.x+

                    cos(angle).toFloat()*radius,

                    center.y+

                    sin(angle).toFloat()*radius

                )

            )

        }

        return list

    }

}