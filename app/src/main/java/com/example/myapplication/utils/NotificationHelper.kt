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

    private const val TAG = "NotificationHelper"

    private const val CHANNEL_MINDFUL = "unscroll_mindful_channel"
    private const val CHANNEL_ALERTS = "unscroll_alerts_channel"

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channels to ensure updates take effect
            try {
                manager.deleteNotificationChannel(CHANNEL_MINDFUL)
                manager.deleteNotificationChannel(CHANNEL_ALERTS)
            } catch (e: Exception) {
                // Channels might not exist yet
            }

            // Create mindful channel with HIGH importance for heads-up notifications
            val mindfulChannel = NotificationChannel(
                CHANNEL_MINDFUL,
                "Mindful Check-ins",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gentle reminders about your scrolling habits"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setShowBadge(true)
                setBypassDnd(true)
            }

            // Create alerts channel with HIGH importance
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "DeepScroll Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for unconscious scrolling behavior"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
                setShowBadge(true)
                setBypassDnd(true)
            }

            manager.createNotificationChannel(mindfulChannel)
            manager.createNotificationChannel(alertsChannel)

            android.util.Log.d(TAG, "Notification channels created with HIGH importance")
        }
    }

    private fun dashboardIntent(context: Context, notificationType: String = "mindful", extraData: Int? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", notificationType)
            extraData?.let { putExtra("reel_count", it) }
        }
        val requestCode = System.currentTimeMillis().toInt()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showMindfulNotification(context: Context, count: Int, isReelsCount: Boolean = false) {
        android.util.Log.d(TAG, "=== SHOWING MINDFUL NOTIFICATION ===")
        android.util.Log.d(TAG, "count=$count, isReelsCount=$isReelsCount")
        android.util.Log.d(TAG, "hasPermission=${hasPermission(context)}")

        if (!hasPermission(context)) {
            android.util.Log.e(TAG, "Notification permission not granted")
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(manager)

        // Check if notifications are enabled for this app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val areNotificationsEnabled = manager.areNotificationsEnabled()
            android.util.Log.d(TAG, "Are notifications enabled for this app? $areNotificationsEnabled")
            if (!areNotificationsEnabled) {
                android.util.Log.e(TAG, "Notifications are disabled for this app")
                return
            }
        }

        val text = if (isReelsCount) {
            "You've viewed $count reel${if (count > 1) "s" else ""} in this session. Take a mindful moment."
        } else {
            "You've been scrolling for $count minute${if (count > 1) "s" else ""}. Consider taking a break."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MINDFUL)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom)
            .setContentTitle("Gentle Reminder 🌿")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(dashboardIntent(context, "mindful", count))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // IMPORTANT: Use unique notification ID based on count
        val notificationId = 1000 + (count % 100)
        android.util.Log.d(TAG, "Calling manager.notify($notificationId)")
        manager.notify(notificationId, notification)
        android.util.Log.d(TAG, "Notification shown successfully with ID: $notificationId")
    }

    fun showReelMilestoneNotification(context: Context, total: Int) {
        showMindfulNotification(context, total, true)
    }

    fun showUnconsciousScrollingNotification(context: Context, type: UnconsciousType) {
        android.util.Log.d(TAG, "=== SHOWING UNCONSCIOUS NOTIFICATION ===")
        android.util.Log.d(TAG, "type=$type, hasPermission=${hasPermission(context)}")

        if (!hasPermission(context)) {
            android.util.Log.e(TAG, "Notification permission not granted")
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(manager)

        val (title, message) = when (type) {
            UnconsciousType.RAPID_SWIPING -> "Fast Scrolling Detected ⚡" to "You're scrolling very quickly. Take a short break to reset."
            UnconsciousType.DEEP_DIVE -> "Deep Scroll Detected 🌀" to "You've been deep scrolling for a while. Time for a mindful pause."
            UnconsciousType.ZONE_OUT -> "Zoned Out Detected 🌫️" to "It seems like you're scrolling without paying attention. Take a breath."
            else -> "Mindful Check-in 🌿" to "Want to take a moment to breathe and reset?"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(dashboardIntent(context, "unconscious"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // Use different notification IDs for different types
        val notificationId = when (type) {
            UnconsciousType.RAPID_SWIPING -> 2001
            UnconsciousType.DEEP_DIVE -> 2002
            UnconsciousType.ZONE_OUT -> 2003
            else -> 2004
        }

        android.util.Log.d(TAG, "Calling manager.notify($notificationId)")
        manager.notify(notificationId, notification)
        android.util.Log.d(TAG, "Unconscious notification shown successfully with ID: $notificationId")
    }

    // Helper function to check notification status
    fun checkNotificationStatus(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        android.util.Log.d(TAG, "=== NOTIFICATION STATUS CHECK ===")
        android.util.Log.d(TAG, "Package: ${context.packageName}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = manager.notificationChannels
            android.util.Log.d(TAG, "Notification channels count: ${channels.size}")
            channels.forEach { channel ->
                android.util.Log.d(TAG, "Channel: ${channel.id}, Importance: ${channel.importance}, Name: ${channel.name}")
            }
        }

        val areNotificationsEnabled = manager.areNotificationsEnabled()
        android.util.Log.d(TAG, "Are notifications enabled for this app? $areNotificationsEnabled")

        val hasPermission = hasPermission(context)
        android.util.Log.d(TAG, "Has POST_NOTIFICATIONS permission? $hasPermission")
    }

    // Test function
    fun testNotification(context: Context) {
        android.util.Log.d(TAG, "=== TEST NOTIFICATION ===")
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(manager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom)
            .setContentTitle("🔔 TEST NOTIFICATION")
            .setContentText("If you see this, notifications are working correctly!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        manager.notify(9999, notification)
        android.util.Log.d(TAG, "Test notification sent with ID: 9999")
    }
}