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
     * New method for unconscious/rapid scrolling detection
     * Uses the same channel and style, just different message
     */
    fun showUnconsciousScrollingNotification(context: Context) {
        if (!hasPermission(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo_photoroom) // you can change to a different icon if you want
            .setContentTitle("Whoa — you're scrolling super fast! ⚡")
            .setContentText("Take a quick breath and slow down a bit?")
            .setContentIntent(dashboardIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt() + 1, notification) // +1 to avoid overwriting mindful notif
    }
}