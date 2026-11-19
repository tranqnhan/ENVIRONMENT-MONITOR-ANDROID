package com.example.environmentmonitorapp.components

import android.R.attr.contentDescription
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun StatusCircleLight(statusColor: Color) {
    Canvas(
        modifier = Modifier
            .size(28.dp)
    ) {
        // Soft outer glow.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    statusColor.copy(alpha = 0.55f),
                    statusColor.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension / 2
            )
        )

        // Solid center.
        drawCircle(
            color = statusColor,
            radius = 5.dp.toPx()
        )
    }
}