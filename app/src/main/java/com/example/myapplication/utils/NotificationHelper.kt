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

    private const val CHANNEL_ID = "unscroll_mindful_channel"

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Mindful Check-ins",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Gentle awareness reminders"
                }
            )
        }
    }

    fun showReelReminderNotification(
        context: Context,
        milestoneReels: Int   // ← changed: this is now the milestone (5,10,15... or 1,2,3...)
    ) {
        if (!hasPermission(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val plural = if (milestoneReels == 1) "reel" else "reels"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom)
            .setContentTitle("Just a gentle check-in 🌿")
            .setContentText("You've watched $milestoneReels $plural so far.")
            .setContentIntent(dashboardIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Use a fixed or predictable ID so updates don't create stacked notifications if desired
        // But using timestamp is fine too if you want separate notifications
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun dashboardIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showMindfulNotification(
        context: Context,
        minutes: Int
    ) {
        if (!hasPermission(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom)
            .setContentTitle("Just a gentle check-in 🌿")
            .setContentText("You’ve been scrolling for about $minutes minutes.")
            .setContentIntent(dashboardIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Updated method for unconscious scrolling detection
     * Shows different messages based on the detected unconscious type
     */
    fun showUnconsciousScrollingNotification(
        context: Context,
        type: UnconsciousType
    ) {
        if (!hasPermission(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        // Customize title and message based on type
        val (title, message) = when (type) {
            UnconsciousType.RAPID_SWIPING -> {
                "Fast scrolling moment ⚡" to
                        "That was a lot of quick swipes in a row. Want to slow down for a second?"
            }

            UnconsciousType.ZONE_OUT -> {
                "Just checking in 🌫️" to
                        "You’ve been scrolling quietly for a bit. How are you feeling right now?"
            }

            UnconsciousType.ROBOTIC -> {
                "Autopilot detected 🤖" to
                        "Your scrolling looks a little repetitive. A small pause can help reset."
            }

            UnconsciousType.DEEP_DIVE -> {
                "Deep scroll happening 🌀" to
                        "You’ve been here for a while. No rush—just a gentle moment to notice."
            }

            UnconsciousType.MINDLESS_BROWSING -> {
                "Mindless scroll moment 😶" to
                        "It seems like scrolling without a clear goal. Want to take a breath?"
            }
        }


        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(dashboardIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Use different notification ID based on type to avoid overwriting
        val notificationId = when (type) {
            UnconsciousType.RAPID_SWIPING -> 1001
            UnconsciousType.ZONE_OUT -> 1002
            UnconsciousType.ROBOTIC -> 1003
            UnconsciousType.DEEP_DIVE -> 1004
            UnconsciousType.MINDLESS_BROWSING -> 1005
        }

        manager.notify(notificationId, notification)
    }
}