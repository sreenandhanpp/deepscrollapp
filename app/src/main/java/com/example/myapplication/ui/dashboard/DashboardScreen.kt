package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    viewModel: MainViewModel
) {
    // 🔹 Scroll count from DataStore
    val scrollCount = viewModel.scrollCount.collectAsState().value

    // 🔹 Local input state for reel interval
    val tempInput = remember {
        mutableStateOf(viewModel.reelsNotifyInterval.value.toString())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // 👋 Greeting
            Text(
                text = "Hello, Mindful Human",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🛡 Aura Protection Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Aura Protection",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Stay intentional while browsing",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Switch(
                        checked = viewModel.isTrackingEnabled.value,
                        onCheckedChange = { viewModel.toggleTracking() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔔 Reel Awareness Section
            Text(
                text = "Reel awareness notification",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tempInput.value,
                onValueChange = { tempInput.value = it },
                label = { Text("Notify every N reels") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    tempInput.value.toIntOrNull()?.let { number ->
                        viewModel.updateReelInterval(number)
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Set")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 📊 Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                StatCard(
                    title = "Scrolls Detected",
                    value = scrollCount.toString(),
                    icon = Icons.Default.Swipe,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Current Streak",
                    value = "5 Days",
                    icon = Icons.Default.FlashOn,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatCard(
                title = "Saved Time",
                value = "1.2h",
                icon = Icons.Default.Timer,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
