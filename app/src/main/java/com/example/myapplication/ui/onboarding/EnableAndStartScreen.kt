package com.example.myapplication.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EnableAndStartScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "You’re all set.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enable gentle reflection to begin noticing your scrolling.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            EnableButton {
                openAccessibilitySettings(context)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "You can turn this off anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ Explicit finish (clear & intentional)
            TextButton(onClick = onFinish) {
                Text("Continue to dashboard")
            }

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun EnableButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(28.dp)
            ),
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
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}
