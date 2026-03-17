package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onOpenNotificationSettings: () -> Unit,
    onOpenYearHeatmap: () -> Unit
){

    val usageMinutesToday by viewModel.totalUsageMinutesToday.collectAsState()
    val deepScrollCount by viewModel.deepScrollCount.collectAsState()
    val monthStats by viewModel.monthStats.collectAsState()
    // 🔢 reel reminder preference
    val notifyAfterReels by viewModel.notifyAfterReels.collectAsState()
    val yearStats by viewModel.yearStats.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val reelsViewedToday = todayStats?.reelsViewed ?: 0

    // input state
    var tempInput by remember { mutableStateOf(notifyAfterReels.toString()) }

    val focusManager = LocalFocusManager.current
    var showSavedMessage by remember { mutableStateOf(false) }
    var savedValue by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Hello 👋",
                    style = MaterialTheme.typography.headlineMedium
                )

                IconButton(
                    onClick = { onOpenNotificationSettings() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notification settings"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            /* ---------------- UNROLL CARD ---------------- */

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        text = "Unscroll",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Quiet awareness while you scroll",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            /* ---------------- REEL REMINDER ---------------- */

            Text(
                text = "Gentle reminder",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tempInput,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        tempInput = newValue
                    }
                },
                label = { Text("Remind me after (reels)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    tempInput.toIntOrNull()?.let { reels ->

                        if (reels > 0) {

                            viewModel.updateNotifyAfterReels(reels)

                            savedValue = reels.toString()

                            showSavedMessage = true

                            coroutineScope.launch {
                                delay(3000)
                                showSavedMessage = false
                            }

                            focusManager.clearFocus()

                            tempInput = reels.toString()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Set")
            }

            if (showSavedMessage) {

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Saved! We'll remind you after $savedValue reels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            /* ---------------- TODAY STATS ---------------- */

            Text(
                text = "Today’s reflection",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                StatCard(
                    title = "Reels watched",
                    value = reelsViewedToday.toString(),
                    icon = Icons.Default.FlashOn,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Deep scroll moments",
                    value = deepScrollCount.toString(),
                    icon = Icons.Default.FlashOn,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Time on Instagram",
                    value =
                        if (usageMinutesToday == 0L) "—"
                        else {
                            val hours = usageMinutesToday / 60
                            val mins = usageMinutesToday % 60
                            if (hours > 0) "${hours}h ${mins}m"
                            else "${mins}m"
                        },
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "This month",
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(
                    onClick = { onOpenYearHeatmap() }
                ) {
                    Text("View all")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MonthHeatmap(stats = monthStats)

            Button(
                onClick = { viewModel.openUserIdScreen() }
            ) {
                Text("Device ID")
            }


            Text(
                text = "Unscroll doesn’t block — it helps you notice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )



            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}