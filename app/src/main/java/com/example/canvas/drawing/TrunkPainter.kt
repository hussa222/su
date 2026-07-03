package com.example.canvas.drawing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.models.CanvasElement

@Composable
fun TrunkPainter(
    element: CanvasElement,
    modifier: Modifier = Modifier
) {

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {

        val w = size.width
        val h = size.height

        rotate(element.rotation) {

            val trunk = Path().apply {

                moveTo(w*0.28f,h)

                cubicTo(
                    w*0.18f,h*0.80f,
                    w*0.22f,h*0.55f,
                    w*0.30f,h*0.10f
                )

                quadraticTo(
                    w*0.50f,
                    -h*0.02f,
                    w*0.70f,
                    h*0.10f
                )

                cubicTo(
                    w*0.78f,
                    h*0.55f,
                    w*0.82f,
                    h*0.80f,
                    w*0.72f,
                    h
                )

                close()

            }

            drawPath(

                path=trunk,

                brush=Brush.verticalGradient(

                    listOf(

                        Color(0xff9C6A3B),

                        Color(0xff7A4E28),

                        Color(0xff5D3920)

                    )

                )

            )

            drawPath(

                path=trunk,

                color=Color(0xff3E2723),

                style=Stroke(2f)

            )

            clipPath(trunk){

                for(i in 0..16){

                    val x=w*(0.18f+i*0.04f)

                    val grain=Path()

                    grain.moveTo(x,h)

                    grain.cubicTo(

                        x-10f,
                        h*0.70f,

                        x+12f,
                        h*0.35f,

                        x-6f,
                        0f

                    )

                    drawPath(

                        grain,

                        Color.Black.copy(alpha=0.08f),

                        style=Stroke(1.2f)

                    )

                }

            }

            for(i in 0..10){

                drawCircle(

                    color=Color(0xff6D4C41),

                    radius=3f+i%2,

                    center=Offset(

                        w*0.35f+(i*13),

                        h*0.20f+(i*18)

                    )

                )

            }

            val style=TextStyle(

                color=Color.White,

                fontWeight=FontWeight.Bold,

                fontSize=element.fontSize.sp

            )

            val names=element.trunkNames

            val top=h*0.18f

            val bottom=h*0.90f

            val step=(bottom-top)/9f

            for(i in 0..9){

                val txt=if(i<names.size) names[9-i] else ""

                if(txt.isNotEmpty()){

                    val result=textMeasurer.measure(

                        txt,

                        style

                    )

                    drawText(

                        textMeasurer,

                        txt,

                        Offset(

                            (w-result.size.width)/2,

                            top+i*step

                        ),

                        style

                    )

                }

            }

        }

    }

}