package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.ScrollDataStore
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

    private val SESSION_RESET_MS = 45_000L
    private val MIN_SESSION_MS = 2_000L   // testing

    /* ================= USER SETTING ================= */

    private var notifyAfterMinutes = 1    // testing default

    /* ================= DEEP SCROLL ================= */

    private var continuousScrollTime = 0L
    private val PAUSE_THRESHOLD_MS = 1_200L
    private val NOTIFICATION_COOLDOWN_MS = 30_000L

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

        /* 1️⃣ Only scroll events */
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        /* 2️⃣ Instagram only */
        if (event.packageName?.toString() != "com.instagram.android") return

        /* 3️⃣ Ignore horizontal & fake scrolls */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) return
            if (event.scrollDeltaY == 0 && event.maxScrollY == 0) return
        }

        val now = System.currentTimeMillis()

        /* ---------- Session reset on inactivity ---------- */
        if (lastScrollTime != 0L && now - lastScrollTime > SESSION_RESET_MS) {
            endSession()
        }

        /* ---------- Start session ---------- */
        if (sessionStartTime == 0L) {
            sessionStartTime = now
            lastScrollTime = now
            return
        }

        /* ---------- Ignore early exploration ---------- */
        if (now - sessionStartTime < MIN_SESSION_MS) {
            lastScrollTime = now
            return
        }

        /* ---------- Accumulate deep scrolling ---------- */
        val delta = now - lastScrollTime
        lastScrollTime = now

        if (delta < PAUSE_THRESHOLD_MS) {
            continuousScrollTime += delta
        } else {
            // natural pause → slow decay, not reset
            continuousScrollTime =
                (continuousScrollTime - 1_000L).coerceAtLeast(0L)
        }

        val thresholdMs = notifyAfterMinutes * 60_000L

        /* ---------- Trigger notification ---------- */
        if (
            !notificationShownThisSession &&
            continuousScrollTime >= thresholdMs &&
            now - lastNotificationTime > NOTIFICATION_COOLDOWN_MS
        ) {
            lastNotificationTime = now
            notificationShownThisSession = true

            CoroutineScope(Dispatchers.IO).launch {
                UsageDataStore.incrementDeepScroll(applicationContext)
            }

            NotificationHelper.showMindfulNotification(
                applicationContext,
                notifyAfterMinutes
            )
        }
    }

    private fun endSession() {
        if (sessionStartTime == 0L) return

        val duration = lastScrollTime - sessionStartTime
        if (duration > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                UsageDataStore.addSessionTime(applicationContext, duration)
            }
        }

        // reset everything
        sessionStartTime = 0L
        lastScrollTime = 0L
        continuousScrollTime = 0L
        notificationShownThisSession = false
    }

    override fun onInterrupt() {}
}


