package com.example.myapplication.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.ui.heatmap.MonthlyHeatmap
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val today = viewModel.today.collectAsStateWithLifecycle().value
    val childId = viewModel.childId.collectAsStateWithLifecycle().value
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.registerIfNeeded() }

    val reelsAnimated = animateIntAsState(today.reelsViewed, label = "reels")

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("DeepScroll Dashboard", style = MaterialTheme.typography.headlineSmall)
        Text("Child ID: $childId")
        Button(onClick = { copyId(context, childId); scope.launch { snack.showSnackbar("Child ID copied") } }) {
            Text("Copy Child ID")
        }
        Text("Minutes today: ${today.usageMinutes}")
        Text("Reels viewed: ${reelsAnimated.value}")
        Text("DeepScroll count: ${today.deepScrollCount}")
        Text("Sessions: ${today.sessions}")
        Text("Intensity: ${"%.2f".format(today.intensityScore)}")
        MonthlyHeatmap(values = List(35) { if (it % 4 == 0) today.deepScrollCount else today.reelsViewed / 10 }, onTap = {})
        SnackbarHost(hostState = snack)
    }
}

private fun copyId(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Child ID", value))
}
