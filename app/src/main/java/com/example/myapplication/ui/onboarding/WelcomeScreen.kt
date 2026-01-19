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
private fun TrustLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun CalmIllustration() {
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {

        // Outer soft glow
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
                )
        )

        // Inner focus dot
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
        )
    }
}

@Composable
fun WelcomeScreen(
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {

            // Main Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    CalmIllustration()

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Notice your scrolling,\nwithout pressure.",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "This app gently helps you become aware of unconscious scrolling — nothing more.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TrustLine("• It never blocks your apps")
                    TrustLine("• You stay fully in control")

                    Spacer(modifier = Modifier.height(28.dp))

                    // 👉 THIS IS THE KEY ADDITION
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Get Started")
                    }
                }
            }
        }
    }
}
