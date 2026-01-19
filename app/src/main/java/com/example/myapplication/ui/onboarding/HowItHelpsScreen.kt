package com.example.myapplication.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HowItHelpsScreen(
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "A gentle nudge,\nonly when it helps.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "The app doesn’t interrupt you — it simply reflects what’s happening, so you can notice it yourself.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            ReflectionCard(
                title = "A simple count",
                description = "Sometimes, you’ll see a small note showing how many reels or shorts you’ve viewed — just a quiet mirror.",
                iconType = ReflectionIconType.COUNT
            )

            Spacer(modifier = Modifier.height(20.dp))

            ReflectionCard(
                title = "A gentle pause",
                description = "At times, the app may suggest taking a brief pause — not as a warning, just a moment to breathe.",
                iconType = ReflectionIconType.PAUSE
            )

            Spacer(modifier = Modifier.weight(1f))

            // 👉 NEXT BUTTON (THIS IS THE KEY ADDITION)
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun ReflectionCard(
    title: String,
    description: String,
    iconType: ReflectionIconType
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            ReflectionIcon(iconType)

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ReflectionIcon(type: ReflectionIconType) {
    val gradientColors = when (type) {
        ReflectionIconType.COUNT ->
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )

        ReflectionIconType.PAUSE ->
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            )
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
        )
    }
}

private enum class ReflectionIconType {
    COUNT,
    PAUSE
}
