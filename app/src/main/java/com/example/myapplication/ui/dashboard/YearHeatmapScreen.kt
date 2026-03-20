package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Month

@Composable
fun YearHeatmapScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val yearStats by viewModel.yearStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header with back button styled like UserIdScreen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Year Overview",
                style = MaterialTheme.typography.headlineSmall
            )

            // Back button styled like in UserIdScreen
            Button(
                onClick = onNavigateBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content - use weight(1f) to take remaining space
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)  // This ensures it takes remaining space without nested scrolling
        ) {
            items((1..12).toList()) { month ->
                Text(
                    text = Month.of(month).name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                MonthHeatmap(
                    stats = yearStats.filter {
                        LocalDate.parse(it.date).monthValue == month
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Add extra space at the bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}