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
fun BranchPainter(
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

            val base = element.thickness * 2.5f
            val tip = element.thickness * 0.55f

            val path = Path().apply {

                moveTo(w / 2 - base, h)

                cubicTo(
                    w * (0.25f + curve),
                    h * 0.70f,
                    w * (0.38f + curve),
                    h * 0.30f,
                    w / 2 - tip,
                    h * 0.18f
                )

                lineTo(
                    w / 2 + tip,
                    h * 0.18f
                )

                cubicTo(
                    w * (0.62f + curve),
                    h * 0.30f,
                    w * (0.75f + curve),
                    h * 0.70f,
                    w / 2 + base,
                    h
                )

                close()

            }

            drawPath(

                path = path,

                brush = Brush.verticalGradient(

                    listOf(

                        Color(0xff8D5A33),

                        Color(0xff6D4425),

                        Color(0xff53351E)

                    )

                )

            )

            drawPath(

                path,

                Color(0xff3E2723),

                style = Stroke(2f)

            )

            clipPath(path) {

                for (i in 0..10) {

                    val x = w * (0.42f + i * 0.015f)

                    val grain = Path()

                    grain.moveTo(x, h)

                    grain.cubicTo(

                        x - 5,
                        h * 0.65f,

                        x + 6,
                        h * 0.30f,

                        x,
                        h * 0.12f

                    )

                    drawPath(

                        grain,

                        Color.White.copy(alpha = 0.10f),

                        style = Stroke(1f)

                    )

                }

            }

            val radius = element.branchCircleDiameter

            drawCircle(

                brush = Brush.radialGradient(

                    listOf(

                        Color(0xff81C784),

                        Color(0xff43A047)

                    )

                ),

                radius = radius,

                center = Offset(

                    w / 2,

                    h * 0.10f

                )

            )

            drawCircle(

                Color.White,

                radius,

                center = Offset(

                    w / 2,

                    h * 0.10f

                ),

                style = Stroke(2f)

            )

            val style = TextStyle(

                color = Color.White,

                fontWeight = FontWeight.Bold,

                fontSize = element.branchCircleTextSize.sp

            )

            val text = element.title

            val result = textMeasurer.measure(

                text,

                style

            )

            drawText(

                textMeasurer,

                text,

                Offset(

                    (w - result.size.width) / 2,

                    h * 0.10f - result.size.height / 2

                ),

                style

            )

        }

    }

}