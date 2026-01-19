package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.dashboard.DashboardScreen
import com.example.myapplication.ui.dashboard.MainViewModel
import com.example.myapplication.ui.onboarding.OnboardingHost
import com.example.myapplication.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AuraTheme {
                val mainViewModel: MainViewModel = viewModel()

                // 🔹 Observe onboarding state
                val onboardingCompleted =
                    mainViewModel.onboardingCompleted.collectAsState().value

                Surface {
                    if (onboardingCompleted) {

                        // ✅ Ask notification permission AFTER onboarding
                        requestNotificationPermission()

                        DashboardScreen(
                            viewModel = mainViewModel
                        )

                    } else {

                        // 🧭 Show onboarding flow
                        OnboardingHost(
                            onFinish = {
                                mainViewModel.completeOnboarding()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}
