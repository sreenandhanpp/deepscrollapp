package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.DailyStatEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun Heatmap(
    stats: List<DailyStatEntity>,
    modifier: Modifier = Modifier
) {
    val statsMap = stats.associateBy { it.date }
    val today = LocalDate.now()
    val weeks = 5 // Show 5 weeks
    
    Column(modifier = modifier) {
        Text(
            text = "Activity",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Days of week labels (simplified)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("", "M", "", "W", "", "F", "").forEach {
                    Text(text = it, fontSize = 10.sp, modifier = Modifier.height(12.dp))
                }
            }
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items((weeks - 1 downTo 0).toList()) { weekOffset ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0..6).forEach { dayOfWeek ->
                            val date = today.minusWeeks(weekOffset.toLong())
                                .with(java.time.DayOfWeek.MONDAY)
                                .plusDays(dayOfWeek.toLong())
                            
                            val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                            val intensity = statsMap[dateStr]?.let {
                                calculateIntensity(it)
                            } ?: 0
                            
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = getIntensityColor(intensity),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateIntensity(stats: DailyStatEntity): Int {
    val total = stats.reelsViewed + stats.usageMinutes
    return when {
        total == 0 -> 0
        total < 10 -> 1
        total < 30 -> 2
        total < 60 -> 3
        else -> 4
    }
}

@Composable
private fun getIntensityColor(intensity: Int): Color {
    val baseColor = MaterialTheme.colorScheme.primary
    return when (intensity) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        1 -> baseColor.copy(alpha = 0.2f)
        2 -> baseColor.copy(alpha = 0.5f)
        3 -> baseColor.copy(alpha = 0.8f)
        else -> baseColor
    }
}
