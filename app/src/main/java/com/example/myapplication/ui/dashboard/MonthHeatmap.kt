package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.analytics.ScrollDailyStats
import java.time.LocalDate

@Composable
fun MonthHeatmap(
    stats: List<ScrollDailyStats>
) {

    val today = LocalDate.now()
    val daysInMonth = today.lengthOfMonth()

    val map = stats.associateBy { it.date }

    var selectedStat by remember { mutableStateOf<ScrollDailyStats?>(null) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    Column {

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            items((1..daysInMonth).toList()) { day ->

                val date = today.withDayOfMonth(day).toString()
                val stat = map[date]

                val reels = stat?.reelsViewed ?: 0

                val color = when {
                    reels == 0 -> MaterialTheme.colorScheme.surfaceVariant
                    reels < 20 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    reels < 50 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    reels < 100 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                    else -> MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(color, RoundedCornerShape(6.dp))
                        .clickable {
                            selectedStat = stat
                            selectedDay = day
                        },
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = day.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedDay != null) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Day $selectedDay",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Reels watched: ${selectedStat?.reelsViewed ?: 0}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Deep scrolls: ${selectedStat?.deepScrollCount ?: 0}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}