package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.Heatmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onOpenNotificationSettings: () -> Unit,
) {
    val stats by viewModel.allStats.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val childId by viewModel.childId.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F0F0F) // Dark background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            HeaderSection(childId) {
                clipboardManager.setText(AnnotatedString(it))
                scope.launch { snackbarHostState.showSnackbar("Child ID copied!") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TodayStatsGrid(
                minutes = todayStats?.usageMinutes ?: 0,
                reels = todayStats?.reelsViewed ?: 0,
                deepScrolls = todayStats?.deepScrollCount ?: 0,
                sessions = todayStats?.sessions ?: 0
            )

            Spacer(modifier = Modifier.height(32.dp))

            IntensityCard(todayStats?.deepScrollCount ?: 0)

            Spacer(modifier = Modifier.height(32.dp))

            Heatmap(stats = stats, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(32.dp))

            SettingsShortcutCard(onOpenNotificationSettings)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HeaderSection(childId: String?, onCopy: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Unscroll",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            childId?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ID: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    IconButton(onClick = { onCopy(it) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun TodayStatsGrid(minutes: Int, reels: Int, deepScrolls: Int, sessions: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                title = "Usage",
                value = "${minutes}m",
                icon = Icons.Default.Timer,
                color = Color(0xFF64B5F6),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Reels",
                value = reels.toString(),
                icon = Icons.Default.PlayCircle,
                color = Color(0xFF81C784),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                title = "DeepScrolls",
                value = deepScrolls.toString(),
                icon = Icons.Default.Psychology,
                color = Color(0xFFFF8A65),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Sessions",
                value = sessions.toString(),
                icon = Icons.Default.History,
                color = Color(0xFFBA68C8),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun IntensityCard(deepScrollCount: Int) {
    val (label, color) = when {
        deepScrollCount == 0 -> "Calm" to Color(0xFF81C784)
        deepScrollCount < 3 -> "Mild" to Color(0xFFFFD54F)
        deepScrollCount < 6 -> "High" to Color(0xFFFF8A65)
        else -> "Intense" to Color(0xFFE57373)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Intensity Level", style = MaterialTheme.typography.bodySmall, color = color)
                Text(text = label, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Speed, null, tint = color, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun SettingsShortcutCard(onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, null, tint = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Notification Settings", color = Color.White, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}
