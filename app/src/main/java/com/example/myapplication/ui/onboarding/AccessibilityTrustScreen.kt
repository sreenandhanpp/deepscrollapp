package com.example.myapplication.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.myapplication.R

/* ---------------- SCREEN ---------------- */

@Composable
fun AccessibilityTrustScreen(
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {

            /* ---------- CENTERED CONTENT ---------- */
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "You can turn this off anytime in settings. You’re always in control.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            /* ---------- BOTTOM CTA ---------- */
            Button(
                onClick = onNext,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Next")
            }
        }
    }
}

/* ---------------- PERMISSION POINT ---------------- */

@Composable
private fun PermissionPoint(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        ),
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

/* ---------------- ICONS ---------------- */

@Composable
private fun SoftCheckIcon() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/* ---------------- HEADER VISUAL ---------------- */

@Composable
private fun TrustHeaderVisual() {
    Box(
        modifier = Modifier
            .size(150.dp)
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

        // App logo (your provided drawable)
        Image(
            painter = painterResource(id = R.drawable.shieldlogo),
            contentDescription = "App logo",
            modifier = Modifier.size(200.dp)
        )
    }
}
