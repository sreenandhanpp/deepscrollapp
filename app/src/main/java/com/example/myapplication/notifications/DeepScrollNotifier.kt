package com.example.myapplication.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.myapplication.R

class DeepScrollNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var lastAlertAt = 0L

    fun ensureChannels() {
        listOf("reel_alerts" to "Reel Alerts", "deep_scroll" to "DeepScroll Alerts", "usage" to "Usage Alerts")
            .forEach { (id, name) -> manager.createNotificationChannel(NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT)) }
    }

    fun notifyWithCooldown(channelId: String, title: String, text: String, cooldownMs: Long = 180_000) {
        val now = System.currentTimeMillis()
        if (now - lastAlertAt < cooldownMs) return
        lastAlertAt = now
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(channelId.hashCode(), notification)
    }
}
