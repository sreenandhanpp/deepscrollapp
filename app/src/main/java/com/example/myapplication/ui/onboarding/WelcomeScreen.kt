package com.example.myapplication.ui.onboarding

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.R

/* ----------------------------- */
/* --------- SHAPE ------------- */
/* ----------------------------- */



/* ----------------------------- */
/* -------- UI PARTS ----------- */
/* ----------------------------- */

@Composable
private fun ArcBackground() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(
                    bottomStart = 240.dp,
                    bottomEnd = 240.dp
                )
            )
    )
}


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
            painter = painterResource(id = R.drawable.transparent_bg_logo),
            contentDescription = "App logo",
            modifier = Modifier.size(200.dp)
        )

    }
}




/* ----------------------------- */
/* -------- MAIN SCREEN -------- */
/* ----------------------------- */

@Composable
fun WelcomeScreen(
    onNext: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Background arc (decorative only)
        ArcBackground()

        // This Box guarantees the card is centered vertically & horizontally
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {

            // Main Card (TRUE vertical center)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
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
                        text = "Notice your scrolling,\ngently and with kindness.",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Unscroll gently helps you become aware of unconscious scrolling.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TrustLine("• It never blocks your apps")
                    TrustLine("• You stay fully in control")

                    Spacer(modifier = Modifier.height(28.dp))

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


