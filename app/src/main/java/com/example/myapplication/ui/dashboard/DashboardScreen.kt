package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalFocusManager


@Composable
fun DashboardScreen(
    viewModel: MainViewModel
) {
    val usageTimeMs = viewModel.totalUsageTime.collectAsState().value
    val deepScrollCount = viewModel.deepScrollCount.collectAsState().value
    val notifyAfterMinutes =
        viewModel.notifyAfterMinutes.collectAsState().value

    val usageMinutes = (usageTimeMs / 60_000f).roundToInt()

    // 🧘 Local input state (prevents instant updates while typing)
    val tempInput = remember {
        mutableStateOf(notifyAfterMinutes.toString())
    }
    val focusManager = LocalFocusManager.current
    val showSavedMessage = remember { mutableStateOf(false) }


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

            Text(
                text = "Hello 👋",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🌿 Unscroll card
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

            // ⏱ Gentle reminder
            Text(
                text = "Gentle reminder",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tempInput.value,
                onValueChange = { tempInput.value = it },
                label = { Text("Remind me after (minutes)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ Set button (intentional action)
            Button(
                onClick = {
                    tempInput.value.toIntOrNull()?.let { minutes ->
                        if (minutes > 0) {
                            viewModel.updateNotifyAfterMinutes(minutes)

                            // ✅ UX fixes
                            focusManager.clearFocus()     // removes blinking cursor
                            showSavedMessage.value = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Set")
            }

            if (showSavedMessage.value) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "We’ll gently remind you after ${notifyAfterMinutes} minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }



            Spacer(modifier = Modifier.height(32.dp))

            // 📊 Reflection
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
                    title = "Time on Instagram",
                    value = if (usageMinutes == 0) "—" else "$usageMinutes min",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Deep scroll moments",
                    value = deepScrollCount.toString(),
                    icon = Icons.Default.FlashOn,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

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

