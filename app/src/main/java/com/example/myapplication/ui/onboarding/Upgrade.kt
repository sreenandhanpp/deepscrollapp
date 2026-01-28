package com.example.myapplication.ui.onboarding

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun UpgradeScreen(
    onStartTrial: () -> Unit,
    onContinueFree: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            /* ---------- HEADER ---------- */
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {

                SoftHeaderVisual()

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Notice more.\nScroll less.",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You’re already building awareness. Premium simply helps you see deeper patterns over time.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            /* ---------- COMPARISON ---------- */
            FeatureComparison()

            Spacer(modifier = Modifier.weight(1f))

            /* ---------- CTA ---------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Button(
                    onClick = onStartTrial,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Start 7-day free trial",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "No payment required today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onContinueFree) {
                    Text(
                        text = "Continue with free version",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------ */
/* ----------------- SECTIONS --------------------- */
/* ------------------------------------------------ */

@Composable
private fun FeatureComparison() {
    Column {

        FeatureCard(
            title = "Free",
            features = listOf(
                "Today’s stats only",
                "Basic gentle reminders",
                "Fast scroll & zone-out alerts",
                "Local on-device storage"
            ),
            highlighted = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            title = "Premium",
            features = listOf(
                "Daily & weekly history",
                "All unconscious types",
                "Smarter detection, fewer false alerts",
                "Notification controls",
                "Cloud sync & restore",
                "Behavioral insights"
            ),
            highlighted = true
        )
    }
}

@Composable
private fun FeatureCard(
    title: String,
    features: List<String>,
    highlighted: Boolean
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (highlighted)
            MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        else
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = if (highlighted)
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (highlighted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            features.forEach { feature ->
                Text(
                    text = "• $feature",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------ */
/* ----------------- VISUAL ----------------------- */
/* ------------------------------------------------ */

@Composable
private fun SoftHeaderVisual() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
        )
    }
}
