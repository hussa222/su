package com.example.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ControlPanel displays a sleek vertical list of zoom adjustments and positioning resets.
 * Positioned as a floating sidebar control, adhering to standard CAD/drawing software layout.
 */
@Composable
fun ControlPanel(
    onZoomIn: (Offset) -> Unit,
    onZoomOut: (Offset) -> Unit,
    onReset: () -> Unit,
    viewportWidth: Float,
    viewportHeight: Float,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val centerPoint = Offset(viewportWidth / 2f, viewportHeight / 2f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom Controls Capsule
        Surface(
            modifier = Modifier
                .testTag("control_panel")
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom In button
                IconButton(
                    onClick = { onZoomIn(centerPoint) },
                    modifier = Modifier
                        .testTag("zoom_in_button")
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "تكبير (Zoom In)",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Dynamic Scale display
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%.1fx", scale),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Zoom Out button
                IconButton(
                    onClick = { onZoomOut(centerPoint) },
                    modifier = Modifier
                        .testTag("zoom_out_button")
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "تصغير (Zoom Out)",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // View Reset Button
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .testTag("reset_canvas_button")
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "إعادة ضبط المركز (Reset Centering)",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
