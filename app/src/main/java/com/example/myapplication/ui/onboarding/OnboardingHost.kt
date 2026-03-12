package com.example.myapplication.ui.onboarding

import androidx.compose.runtime.*

@Composable
fun OnboardingHost(
    onFinish: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    when (step) {
        0 -> WelcomeScreen(
            onNext = { step++ }
        )

        1 -> HowItHelpsScreen(
            onNext = { step++ }
        )

        2 -> AccessibilityTrustScreen(
            onNext = { step++ }
        )

        3 -> EnableAndStartScreen(
            onFinish = onFinish
        )
    }
}
