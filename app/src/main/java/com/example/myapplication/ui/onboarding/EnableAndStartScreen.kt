package com.example.myapplication.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.myapplication.R

@Composable
fun EnableAndStartScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {

            /* ---------- CENTER CONTENT ---------- */
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                EnableHeaderVisual()

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "You’re all set.",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enable gentle reflection to begin noticing your scrolling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "You stay in control and can turn this off anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            /* ---------- BOTTOM ACTIONS ---------- */
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // PRIMARY ACTION
                Button(
                    onClick = {
                        openAccessibilitySettings(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Enable and start",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SECONDARY ACTION (DASHBOARD)
                TextButton(
                    onClick = onFinish
                ) {
                    Text(
                        text = "Move to dashboard",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/* ---------- HEADER VISUAL ---------- */

@Composable
private fun EnableHeaderVisual() {
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
        Image(
            painter = painterResource(id = R.drawable.enablelogo),
            contentDescription = "Enable illustration",
            modifier = Modifier.size(200.dp)
        )
    }
}

/* ---------- SYSTEM ACTION ---------- */

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}
