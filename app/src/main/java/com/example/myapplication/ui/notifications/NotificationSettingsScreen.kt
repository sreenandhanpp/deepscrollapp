package com.example.myapplication.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.dashboard.MainViewModel

@Composable
fun NotificationSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    // 🔗 ViewModel states
    val timeReminder by viewModel.timeReminderEnabled.collectAsState()
    val rapidSwipe by viewModel.rapidSwipeEnabled.collectAsState()
    val zoneOut by viewModel.zoneOutEnabled.collectAsState()
    val robotic by viewModel.roboticEnabled.collectAsState()
    val deepDive by viewModel.deepDiveEnabled.collectAsState()
    val mindless by viewModel.mindlessEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Spacer(modifier = Modifier.height(24.dp))
















































































































































































































































































































        // 🔙 Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose how Unscroll gently checks in with you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        NotificationToggle(
            title = "Gentle time reminder ⏱️",
            description = "Based on the minutes you set.",
            checked = timeReminder,
            onCheckedChange = viewModel::setTimeReminderEnabled
        )

        NotificationToggle(
            title = "Fast scrolling moment ⚡",
            description = "When swiping becomes very quick.",
            checked = rapidSwipe,
            onCheckedChange = viewModel::setRapidSwipeEnabled
        )

        NotificationToggle(
            title = "Zone out 🌫️",
            description = "Scrolling without interaction.",
            checked = zoneOut,
            onCheckedChange = viewModel::setZoneOutEnabled
        )

        NotificationToggle(
            title = "Autopilot detected 🤖",
            description = "Repetitive scrolling patterns.",
            checked = robotic,
            onCheckedChange = viewModel::setRoboticEnabled
        )

        NotificationToggle(
            title = "Deep scroll happening 🌀",
            description = "Long uninterrupted sessions.",
            checked = deepDive,
            onCheckedChange = viewModel::setDeepDiveEnabled
        )

        NotificationToggle(
            title = "Mindless browsing 😶",
            description = "Scrolling without clear intent.",
            checked = mindless,
            onCheckedChange = viewModel::setMindlessEnabled
        )

        Spacer(modifier = Modifier.weight(1f))

        // Optional back button at bottom (nice UX)
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
