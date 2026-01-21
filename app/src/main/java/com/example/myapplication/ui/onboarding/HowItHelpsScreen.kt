package com.example.myapplication.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R

/* ---------------- APP LOGO ---------------- */

@Composable
private fun AppLogoIllustration() {
    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
        )

        Image(
            painter = painterResource(id = R.drawable.howitsworkslogo),
            contentDescription = "App logo",
            modifier = Modifier.size(200.dp)
        )
    }
}

/* ---------------- SCREEN ---------------- */

@Composable
fun HowItHelpsScreen(
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

            /* -------- CENTERED CONTENT -------- */
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                AppLogoIllustration()

                Spacer(modifier = Modifier.height(20.dp))

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
            }

            /* -------- BOTTOM CTA -------- */
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

/* ---------------- REFLECTION CARD ---------------- */

@Composable
private fun ReflectionCard(
    title: String,
    description: String,
    iconType: ReflectionIconType
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        ),
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

/* ---------------- ICON ---------------- */

@Composable
private fun ReflectionIcon(type: ReflectionIconType) {

    val (icon, glowAlpha) = when (type) {
        ReflectionIconType.COUNT ->
            Icons.Outlined.FormatListNumbered to 0.35f
        ReflectionIconType.PAUSE ->
            Icons.Outlined.PauseCircleOutline to 0.25f
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
    }
}

/* ---------------- ENUM ---------------- */

private enum class ReflectionIconType {
    COUNT,
    PAUSE
}
