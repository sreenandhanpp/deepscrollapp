package com.example.myapplication.ui.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MonthlyHeatmap(values: List<Int>, onTap: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEachIndexed { index, value ->
                    val color = when {
                        value >= 20 -> Color(0xFF4ADE80)
                        value >= 10 -> Color(0xFF22C55E)
                        value >= 5 -> Color(0xFF15803D)
                        value > 0 -> Color(0xFF14532D)
                        else -> Color(0xFF1F2937)
                    }
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color, RoundedCornerShape(4.dp))
                            .clickable { onTap(index) }
                    )
                }
            }
        }
    }
    Text("Tap cells for day detail")
}
