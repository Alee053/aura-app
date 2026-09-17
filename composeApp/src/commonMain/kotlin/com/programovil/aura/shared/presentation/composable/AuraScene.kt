package com.programovil.aura.shared.presentation.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun AuraScene(kind: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Canvas(modifier.aspectRatio(1f)) {
        val u = size.minDimension / 240f
        scale(u, u, pivot = Offset.Zero) {
            if (!c.highContrast) drawCircle(c.primaryContainer, 102f, Offset(120f, 120f))
            drawArc(c.primary, -110f, 280f, false, Offset(14f, 14f), Size(212f, 212f),
                style = Stroke(1.5f, cap = StrokeCap.Round))
            drawCircle(c.accent, 7f, Offset(209f, 66f))
            rotate(-6f, Offset(120f, 120f)) {
                drawRoundRect(c.surface, Offset(47f, 48f), Size(150f, 150f), CornerRadius(20f))
                drawRoundRect(c.controlOutline, Offset(47f, 48f), Size(150f, 150f), CornerRadius(20f), style = Stroke(1.5f))
                when(kind) {
                    1 -> repeat(3) { i ->
                        val y = 88f + i * 35f
                        drawCircle(if (i == 0) c.primary else c.surfaceVariant, 9f, Offset(75f, y))
                        if (i == 0) {
                            drawLine(c.onPrimary, Offset(71f, y), Offset(74f, y + 3), 2f, StrokeCap.Round)
                            drawLine(c.onPrimary, Offset(74f, y + 3), Offset(80f, y - 4), 2f, StrokeCap.Round)
                        }
                        drawLine(c.textSecondary, Offset(97f, y), Offset(if (i == 2) 155f else 173f, y), 3f, StrokeCap.Round)
                    }
                    2 -> {
                        drawLine(c.textPrimary, Offset(68f, 78f), Offset(150f, 78f), 5f, StrokeCap.Round)
                        repeat(7) { i -> drawCircle(if (i < 4) c.primary else c.surfaceVariant, 6f, Offset(70f + i * 17f, 117f)) }
                        drawRoundRect(c.surfaceVariant, Offset(68f, 145f), Size(107f, 8f), CornerRadius(4f))
                        drawRoundRect(c.primary, Offset(68f, 145f), Size(70f, 8f), CornerRadius(4f))
                    }
                    3 -> {
                        drawLine(c.primary, Offset(70f, 80f), Offset(150f, 80f), 6f, StrokeCap.Round)
                        repeat(4) { i -> drawLine(c.textSecondary, Offset(70f, 108f + i * 18), Offset(if (i == 3) 136f else 175f, 108f + i * 18), 2f, StrokeCap.Round) }
                        drawLine(c.primary, Offset(175f, 170f), Offset(191f, 134f), 6f, StrokeCap.Round)
                    }
                    else -> {
                        drawRoundRect(c.hero, Offset(63f, 68f), Size(118f, 54f), CornerRadius(12f))
                        drawCircle(c.accent, 12f, Offset(157f, 95f))
                        drawLine(c.onHero, Offset(77f, 94f), Offset(118f, 94f), 4f, StrokeCap.Round)
                        drawRoundRect(c.primaryContainer, Offset(63f, 134f), Size(52f, 46f), CornerRadius(10f))
                        drawRoundRect(c.surfaceVariant, Offset(127f, 134f), Size(54f, 46f), CornerRadius(10f))
                    }
                }
            }
        }
    }
}
