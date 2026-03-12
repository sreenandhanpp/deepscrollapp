package com.example.myapplication.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape   // ✅ ADD THIS
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
