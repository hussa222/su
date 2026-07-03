package com.example.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PanelEdge { START, END }

/**
 * غلاف عام لأي لوحة جانبية (خصائص، طبقات...) يسمح بطيّها/إخفائها،
 * مع بقاء سهم صغير ثابت على حافة الشاشة لإرجاعها عند الضغط عليه.
 */
@Composable
fun CollapsibleSidePanel(
    isCollapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    edge: PanelEdge = PanelEdge.END,
    modifier: Modifier = Modifier,
    panelWidth: Dp = 380.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = if (edge == PanelEdge.END) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        AnimatedVisibility(
            visible = !isCollapsed,
            enter = slideInHorizontally(
                initialOffsetX = { if (edge == PanelEdge.END) it else -it },
                animationSpec = tween(220)
            ) + fadeIn(tween(220)),
            exit = slideOutHorizontally(
                targetOffsetX = { if (edge == PanelEdge.END) it else -it },
                animationSpec = tween(200)
            ) + fadeOut(tween(180))
        ) {
            Box(modifier = Modifier.widthIn(max = panelWidth)) {
                content()
            }
        }

        val shape = if (edge == PanelEdge.END) {
            RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
        } else {
            RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp)
        }

        Surface(
            modifier = Modifier
                .testTag("panel_arrow_tab")
                .clip(shape)
                .clickable(onClick = onToggleCollapsed)
                .size(width = 22.dp, height = 56.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp,
            shape = shape
        ) {
            Box(contentAlignment = Alignment.Center) {
                val showLeftArrow = (edge == PanelEdge.END && isCollapsed) || (edge == PanelEdge.START && !isCollapsed)
                Icon(
                    imageVector = if (showLeftArrow) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                    contentDescription = if (isCollapsed) "إظهار اللوحة" else "إخفاء اللوحة",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
