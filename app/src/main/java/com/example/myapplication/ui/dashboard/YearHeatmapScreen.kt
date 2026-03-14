package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Month

@Composable
fun YearHeatmapScreen(
    viewModel: MainViewModel
) {

    val yearStats by viewModel.yearStats.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
    }
}