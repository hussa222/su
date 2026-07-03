package com.example.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * HomeScreen introduces the application.
 * Styled with a clean blueprint visual card and M3 buttons.
 */
@Composable
fun HomeScreen(
    onCreateNewProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header (Logo and Title)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // Animated App Icon Badge - Clean Minimalism Style
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DesignServices,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Title (Arabic & English)
                Text(
                    text = "لوحة التصميم التفاعلية",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "CANVAS ENGINE V1.0 • ADVANCED GRAPHICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "محرك الرسم الهندسي المتقدم",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // 2. Beautiful Interactive Vector Art (Drawing Preview)
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + scaleIn(initialScale = 0.9f)
            ) {
                BlueprintArtCard()
            }

            // 3. Bottom Action Buttons Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Button 1: Create New Project (Main Action)
                Button(
                    onClick = onCreateNewProject,
                    modifier = Modifier
                        .testTag("create_new_project_button")
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "إنشاء مشروع جديد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Button 2: Import Project (Secondary Action)
                OutlinedButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "استيراد المشاريع سيتوفر في مراحل تطوير قادمة",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier
                        .testTag("import_project_button")
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Import",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "استيراد مشروع خارجي",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Small decorative phase badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Engine Core",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "محرك اللوحة الرسومية نشط • 60 FPS Engine",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * BlueprintArtCard draws a beautiful modern geometric blueprint preview in Compose.
 * Demonstrates grids, circles, coordinate scales, and designer elements.
 */
@Composable
private fun BlueprintArtCard() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(vertical = 16.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Draw custom design blueprint vector
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 1. Draw geometric grid background
                val step = 30.dp.toPx()
                for (x in 0..(w / step).toInt()) {
                    drawLine(
                        color = outlineVariant.copy(alpha = 0.15f),
                        start = Offset(x * step, 0f),
                        end = Offset(x * step, h),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..(h / step).toInt()) {
                    drawLine(
                        color = outlineVariant.copy(alpha = 0.15f),
                        start = Offset(0f, y * step),
                        end = Offset(w, y * step),
                        strokeWidth = 1f
                    )
                }

                // 2. Draw coordinate axis lines in card
                drawLine(
                    color = primaryColor.copy(alpha = 0.25f),
                    start = Offset(w / 2f, 0f),
                    end = Offset(w / 2f, h),
                    strokeWidth = 2f
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.25f),
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = 2f
                )

                // 3. Draw concentric blueprint circles
                drawCircle(
                    color = primaryColor.copy(alpha = 0.08f),
                    radius = 80.dp.toPx(),
                    center = Offset(w / 2f, h / 2f)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = 80.dp.toPx(),
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.3f),
                    radius = 40.dp.toPx(),
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(width = 1.5f)
                )

                // 4. Draw interactive node points
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = Offset(w / 2f, h / 2f)
                )
                drawCircle(
                    color = secondaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(w / 2f - 80.dp.toPx(), h / 2f)
                )
                drawCircle(
                    color = tertiaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(w / 2f + 80.dp.toPx(), h / 2f)
                )

                // 5. Connecting design lines
                drawLine(
                    color = secondaryColor.copy(alpha = 0.4f),
                    start = Offset(w / 2f, h / 2f),
                    end = Offset(w / 2f - 80.dp.toPx(), h / 2f),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = tertiaryColor.copy(alpha = 0.4f),
                    start = Offset(w / 2f, h / 2f),
                    end = Offset(w / 2f + 80.dp.toPx(), h / 2f),
                    strokeWidth = 1.5f
                )
            }

            // Small badge showing "Interactive Viewer"
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "محاكاة الفضاء اللا نهائي",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
