package com.example.canvas.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
fun LeafPainter(
    element: CanvasElement,
    modifier: Modifier = Modifier
) {

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {

        val w = size.width
        val h = size.height

        rotate(element.rotation) {

            val curve = element.curvature / 100f

            val leaf = Path().apply {

                moveTo(w / 2f, 0f)

                cubicTo(
                    w * 1.02f,
                    h * 0.20f,

                    w * 0.90f,
                    h * 0.80f,

                    w / 2f,
                    h
                )

                cubicTo(
                    w * 0.10f,
                    h * 0.80f,

                    -w * 0.02f,
                    h * 0.20f,

                    w / 2f,
                    0f
                )

                close()

            }

            drawPath(

                path = leaf,

                brush = Brush.verticalGradient(

                    listOf(

                        Color(0xff81C784),

                        Color(0xff4CAF50),

                        Color(0xff2E7D32)

                    )

                )

            )

            drawPath(

                leaf,

                Color(0xff1B5E20),

                style = Stroke(2f)

            )

            clipPath(leaf) {

                drawLine(

                    color = Color.White.copy(alpha = .30f),

                    start = Offset(
                        w / 2,
                        0f
                    ),

                    end = Offset(
                        w / 2,
                        h
                    ),

                    strokeWidth = 3f

                )

                for (i in 1..6) {

                    val y = h * (i / 7f)

                    drawLine(

                        color = Color.White.copy(alpha = .18f),

                        start = Offset(
                            w / 2,
                            y
                        ),

                        end = Offset(
                            w * .18f,
                            y - 18
                        ),

                        strokeWidth = 1.5f

                    )

                    drawLine(

                        color = Color.White.copy(alpha = .18f),

                        start = Offset(
                            w / 2,
                            y
                        ),

                        end = Offset(
                            w * .82f,
                            y - 18
                        ),

                        strokeWidth = 1.5f

                    )

                }

                for(i in 0..20){

                    drawCircle(

                        Color.White.copy(alpha=.03f),

                        radius=2f,

                        center=Offset(

                            w*(0.15f+i*0.03f),

                            h*(0.15f+i*0.03f)

                        )

                    )

                }

            }

            val style = TextStyle(

                color = Color.White,

                fontWeight = FontWeight.Bold,

                fontSize = element.fontSize.sp

            )

            val result = textMeasurer.measure(

                element.title,

                style

            )

            drawText(

                textMeasurer,

                element.title,

                Offset(

                    (w-result.size.width)/2,

                    (h-result.size.height)/2

                ),

                style

            )

        }

    }

}