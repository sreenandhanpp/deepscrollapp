package com.example.myapplication.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.service.detector.UnconsciousType

object NotificationHelper {

    private const val CHANNEL_MINDUL = "unscroll_mindful_channel"
    private const val CHANNEL_ALERTS = "unscroll_alerts_channel"

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_MINDUL, "Mindful Check-ins", NotificationManager.IMPORTANCE_DEFAULT)
            )
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, "DeepScroll Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun dashboardIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    fun showMindfulNotification(context: Context, count: Int, isReelsCount: Boolean = false) {
        if (!hasPermission(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(manager)

        val text = if (isReelsCount) "You've viewed $count reels in this session." 
                   else "You've been scrolling for $count minutes."

        val notification = NotificationCompat.Builder(context, CHANNEL_MINDUL)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom)
            .setContentTitle("Gentle Reminder 🌿")
            .setContentText(text)
            .setContentIntent(dashboardIntent(context))
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }

    fun showReelMilestoneNotification(context: Context, total: Int) {
        // Implementation for total daily milestones
    }

    fun showUnconsciousScrollingNotification(context: Context, type: UnconsciousType) {
        if (!hasPermission(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(manager)

        val (title, message) = when (type) {
            UnconsciousType.RAPID_SWIPING -> "Fast scrolling moment ⚡" to "Take a short break from scrolling."
            UnconsciousType.DEEP_DIVE -> "Deep scroll happening 🌀" to "You've been deep scrolling for a while."
            else -> "Mindful check-in 🌫️" to "Want to take a breath?"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(2, notification)
    }
}
