package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.UsageDataStore
import com.example.myapplication.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class ScrollDetectorService : AccessibilityService() {

    /* ================= SESSION ================= */

    private var sessionStartTime = 0L
    private var lastScrollTime = 0L
    private var accumulatedSessionTime = 0L

    private val SESSION_RESET_MS = 20_000L
    private val MIN_SESSION_MS = 2_000L

    /* ================= USER SETTING ================= */

    private var notifyAfterMinutes = 1

    /* ================= NOTIFICATION ================= */

    private val NOTIFICATION_COOLDOWN_MS = 15_000L
    private var lastNotificationTime = 0L
    private var notificationShownThisSession = false

    override fun onServiceConnected() {
        super.onServiceConnected()

        CoroutineScope(Dispatchers.IO).launch {
            NotificationSettingsStore
                .notifyAfterMinutesFlow(applicationContext)
                .collect {
                    notifyAfterMinutes = it.coerceAtLeast(1)
                }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        if (event.packageName?.toString() != "com.instagram.android") return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) return
            if (event.scrollDeltaY == 0 && event.maxScrollY == 0) return
        }

        val now = System.currentTimeMillis()

        /* ---------- Reset session ---------- */
        if (lastScrollTime != 0L && now - lastScrollTime > SESSION_RESET_MS) {
            endSession()
        }

        /* ---------- Start session ---------- */
        if (sessionStartTime == 0L) {
            sessionStartTime = now
            lastScrollTime = now
            return
        }

        /* ---------- Accumulate time properly ---------- */
        val delta = now - lastScrollTime
        lastScrollTime = now

        // Count time even if user is watching reels
        accumulatedSessionTime += delta

        if (accumulatedSessionTime < MIN_SESSION_MS) return

        val thresholdMs = notifyAfterMinutes * 60_000L

        if (
            !notificationShownThisSession &&
            accumulatedSessionTime >= thresholdMs &&
            now - lastNotificationTime > NOTIFICATION_COOLDOWN_MS
        ) {
            notificationShownThisSession = true
            lastNotificationTime = now

            CoroutineScope(Dispatchers.IO).launch {
                UsageDataStore.addSessionTime(
                    applicationContext,
                    accumulatedSessionTime
                )
            }

            NotificationHelper.showMindfulNotification(
                applicationContext,
                notifyAfterMinutes
            )
        }
    }

    private fun endSession() {
        if (sessionStartTime == 0L) return

        CoroutineScope(Dispatchers.IO).launch {
            UsageDataStore.addSessionTime(
                applicationContext,
                accumulatedSessionTime
            )
        }

        sessionStartTime = 0L
        lastScrollTime = 0L
        accumulatedSessionTime = 0L
        notificationShownThisSession = false
    }

    override fun onInterrupt() {}
}

