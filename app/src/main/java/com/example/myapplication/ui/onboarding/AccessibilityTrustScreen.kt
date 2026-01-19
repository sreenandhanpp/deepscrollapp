package com.example.myapplication.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AccessibilityTrustScreen(
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

            Spacer(modifier = Modifier.height(36.dp))

            TrustHeaderVisual()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Why we ask for this",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "To gently notice scrolling, the app needs a way to detect scroll activity on your screen — nothing more.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            PermissionPoint("Detects scrolling activity only")
            PermissionPoint("Does not read messages, text, or content")
            PermissionPoint("All processing happens on your device")

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "You can turn this off anytime in settings. You’re always in control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            GentleContinueHint(onNext)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionPoint(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            SoftCheckIcon()

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SoftCheckIcon() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
        )
    }
}

@Composable
private fun TrustHeaderVisual() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
        )
    }
}

@Composable
private fun GentleContinueHint(onNext: () -> Unit) {
    Text(
        text = "Continue when you’re ready",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable { onNext() }   // ✅ THIS WAS MISSING
            .padding(8.dp)
    )
}
