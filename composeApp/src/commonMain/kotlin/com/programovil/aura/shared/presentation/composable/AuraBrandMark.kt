package com.programovil.aura.shared.presentation.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import kotlin.math.*

@Composable
fun AuraBrandMark(modifier: Modifier = Modifier, color: Color = AppTheme.colors.primary) {
    Canvas(modifier.size(40.dp)) {
        val unit = size.minDimension / 24f
        val center = center
        val radius = 8 * unit
        drawArc(color, -90f, 300f, false,
            topLeft = center - Offset(radius, radius), size = Size(radius * 2, radius * 2),
            style = Stroke(2.4f * unit, cap = StrokeCap.Round))
        val angle = 210f * PI.toFloat() / 180f
        drawCircle(color, 1.8f * unit, center + Offset(cos(angle) * radius, sin(angle) * radius))
    }
}
