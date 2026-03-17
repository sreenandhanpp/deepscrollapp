package com.example.myapplication.ui.settings


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.sync.DeviceIdManager
import kotlinx.coroutines.launch

@Composable
fun UserIdScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var deviceId by remember { mutableStateOf("Loading...") }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deviceId = DeviceIdManager.getDeviceId(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Your Device ID",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text = deviceId,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {

                        val clipboard = context
                            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("device_id", deviceId)
                        )

                        copied = true

                        coroutineScope.launch {
                            kotlinx.coroutines.delay(2000)
                            copied = false
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy ID")
                }

                if (copied) {

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Copied to clipboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack
        ) {
            Text("Back")
        }
    }
}