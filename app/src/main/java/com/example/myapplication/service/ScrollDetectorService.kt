package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    private var lastEventTime = 0L           // Last time ANY Instagram event happened
    private var accumulatedSessionTime = 0L

    private val SESSION_RESET_MS = 20_000L
    private val MIN_SESSION_MS = 2_000L

    /* ================= MINDFUL NOTIFICATION ================= */
    private var notifyAfterMinutes = 1
    private val NOTIFICATION_COOLDOWN_MS = 10_000L
    private var lastNotificationTime = 0L
    private var lastNotifiedMinute = 0
    private var wasDeepSession = false

    /* ================= RAPID SCROLLING DETECTION ================= */
    private var rapidScrollStreak = 0
    private var lastRapidScrollTime = 0L

    private val RAPID_SCROLL_THRESHOLD_MS = 500L
    private val RAPID_SCROLL_STREAK_LIMIT = 3
    private val UNCONSCIOUS_COOLDOWN_MS = 45_000L
    private var lastUnconsciousNotificationTime = 0L

    /* ================= IDLE TIMEOUT ================= */
    private val handler = Handler(Looper.getMainLooper())
    private var endSessionRunnable: Runnable? = null

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
        val packageName = event.packageName?.toString() ?: return

        // Early exit if not Instagram → end session if leaving
        if (packageName != "com.instagram.android") {
            if (sessionStartTime != 0L) endSession()
            return
        }

        val now = System.currentTimeMillis()

        val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        if (isScrollEvent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) return
                if (event.scrollDeltaY == 0 && event.maxScrollY == 0) return
            }
        }

        // Reset if idle
        if (lastEventTime != 0L && now - lastEventTime > SESSION_RESET_MS) {
            endSession()
        }

        // Start session on first event
        if (sessionStartTime == 0L) {
            sessionStartTime = now
            lastEventTime = now
            lastNotifiedMinute = 0
            rapidScrollStreak = 0
            lastRapidScrollTime = 0L
            wasDeepSession = false
        } else {
            // Accumulate real elapsed time (works even without scrolls!)
            val delta = now - lastEventTime
            accumulatedSessionTime += delta
            lastEventTime = now

            // Rapid scroll detection ONLY on scroll events
            if (isScrollEvent) {
                val timeSinceLastScroll = now - lastRapidScrollTime
                if (timeSinceLastScroll < RAPID_SCROLL_THRESHOLD_MS) {
                    rapidScrollStreak++
                } else {
                    rapidScrollStreak = 1
                }
                lastRapidScrollTime = now

                if (rapidScrollStreak >= RAPID_SCROLL_STREAK_LIMIT &&
                    now - lastUnconsciousNotificationTime > UNCONSCIOUS_COOLDOWN_MS
                ) {
                    lastUnconsciousNotificationTime = now
                    NotificationHelper.showUnconsciousScrollingNotification(applicationContext)
                }
            }
        }

        if (accumulatedSessionTime < MIN_SESSION_MS) {
            scheduleSessionTimeout()
            return
        }

        // Mindful notification
        val currentMinutes = (accumulatedSessionTime / 60_000L).toInt()

        if (
            currentMinutes >= notifyAfterMinutes &&
            currentMinutes % notifyAfterMinutes == 0 &&
            currentMinutes != lastNotifiedMinute &&
            now - lastNotificationTime > NOTIFICATION_COOLDOWN_MS
        ) {
            lastNotifiedMinute = currentMinutes
            lastNotificationTime = now
            wasDeepSession = true

            NotificationHelper.showMindfulNotification(
                applicationContext,
                currentMinutes
            )
        }

        scheduleSessionTimeout()
    }

    private fun scheduleSessionTimeout() {
        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = Runnable { endSession() }
        handler.postDelayed(endSessionRunnable!!, SESSION_RESET_MS)
    }

    private fun endSession() {
        if (sessionStartTime == 0L) return

        if (accumulatedSessionTime >= MIN_SESSION_MS) {
            CoroutineScope(Dispatchers.IO).launch {
                // Save time in milliseconds (UsageDataStore converts to minutes)
                UsageDataStore.addSessionTime(applicationContext, accumulatedSessionTime)

                if (wasDeepSession) {
                    UsageDataStore.incrementDeepScroll(applicationContext)
                }
            }
        }

        // Reset all
        sessionStartTime = 0L
        lastEventTime = 0L
        accumulatedSessionTime = 0L
        lastNotifiedMinute = 0
        lastNotificationTime = 0L
        rapidScrollStreak = 0
        lastRapidScrollTime = 0L
        lastUnconsciousNotificationTime = 0L
        wasDeepSession = false

        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = null
    }

    override fun onInterrupt() = endSession()
    override fun onDestroy() {
        endSession()
        super.onDestroy()
    }
}