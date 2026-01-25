package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.UsageDataStore
import com.example.myapplication.service.detector.UnconsciousScrollingDetector
import com.example.myapplication.service.detector.UnconsciousType
import com.example.myapplication.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────
// Main Accessibility Service
// ─────────────────────────────────────────────────────────────
class ScrollDetectorService : AccessibilityService() {

    /* ================= SESSION ================= */

    private var sessionStartTime = 0L
    private var lastEventTime = 0L
    private var accumulatedSessionTime = 0L

    /** Tracks how much time we have ALREADY written to DataStore */
    private var lastPersistedMs = 0L

    private val SESSION_RESET_MS = 90_000L   // 1.5 min idle = new session
    private val MIN_SESSION_MS = 2_000L      // ignore micro sessions

    /* ================= MINDFUL NOTIFICATION ================= */

    private var notifyAfterMinutes = 1
    private val NOTIFICATION_COOLDOWN_MS = 10_000L
    private var lastNotificationTime = 0L
    private var nextNotifyMinute = 1

    /* ================= RAPID SCROLL (fallback) ================= */

    private var rapidScrollStreak = 0
    private var lastRapidScrollTime = 0L

    private val RAPID_SCROLL_THRESHOLD_MS = 500L
    private val RAPID_SCROLL_STREAK_LIMIT = 3
    private val UNCONSCIOUS_COOLDOWN_MS = 45_000L
    private var lastUnconsciousNotificationTime = 0L

    /* ================= IDLE HANDLER ================= */

    private val handler = Handler(Looper.getMainLooper())
    private var endSessionRunnable: Runnable? = null

    /* ================= DETECTOR ================= */

    private lateinit var unconsciousDetector: UnconsciousScrollingDetector

    // ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()

        unconsciousDetector = UnconsciousScrollingDetector(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            NotificationSettingsStore
                .notifyAfterMinutesFlow(applicationContext)
                .collect {
                    notifyAfterMinutes = it.coerceAtLeast(1)
                    nextNotifyMinute = notifyAfterMinutes
                }
        }
    }

    // ─────────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        /* ---- Leave Instagram → end session ---- */
        if (pkg != "com.instagram.android") {
            endSession()
            return
        }

        val now = System.currentTimeMillis()

        unconsciousDetector.updateLastEventTime(now)
        unconsciousDetector.onAccessibilityEvent(event, now)

        val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        /* ---- Filter fake / horizontal scrolls ---- */
        if (isScrollEvent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) return
            if (event.scrollDeltaY == 0 && event.maxScrollY == 0) return
        }

        /* ---- Idle reset ---- */
        if (lastEventTime != 0L && now - lastEventTime > SESSION_RESET_MS) {
            endSession()
        }

        /* ---- Session start ---- */
        if (sessionStartTime == 0L) {
            sessionStartTime = now
            lastEventTime = now
            accumulatedSessionTime = 0L
            lastPersistedMs = 0L
            nextNotifyMinute = notifyAfterMinutes
            rapidScrollStreak = 0
            lastRapidScrollTime = 0L
            return
        }

        /* ---- Accumulate time ---- */
        val delta = now - lastEventTime
        accumulatedSessionTime += delta
        lastEventTime = now

        /* ---- Persist usage every 60s ---- */
        val deltaSincePersist = accumulatedSessionTime - lastPersistedMs
        if (deltaSincePersist >= 60_000L) {
            CoroutineScope(Dispatchers.IO).launch {
                UsageDataStore.addSessionTime(applicationContext, deltaSincePersist)
            }
            lastPersistedMs += deltaSincePersist
        }

        /* ---- Rapid swipe fallback ---- */
        if (isScrollEvent) {
            val sinceLast = now - lastRapidScrollTime
            rapidScrollStreak =
                if (sinceLast < RAPID_SCROLL_THRESHOLD_MS) rapidScrollStreak + 1 else 1
            lastRapidScrollTime = now

            if (
                rapidScrollStreak >= RAPID_SCROLL_STREAK_LIMIT &&
                now - lastUnconsciousNotificationTime > UNCONSCIOUS_COOLDOWN_MS
            ) {
                lastUnconsciousNotificationTime = now
                NotificationHelper.showUnconsciousScrollingNotification(
                    applicationContext,
                    UnconsciousType.RAPID_SWIPING
                )

                CoroutineScope(Dispatchers.IO).launch {
                    UsageDataStore.incrementDeepScroll(applicationContext)
                }
            }
        }

        if (accumulatedSessionTime < MIN_SESSION_MS) {
            scheduleSessionTimeout()
            return
        }

        /* ---- Time-based mindful notification ---- */
        val currentMinutes = (accumulatedSessionTime / 60_000L).toInt()

        if (
            currentMinutes >= nextNotifyMinute &&
            now - lastNotificationTime > NOTIFICATION_COOLDOWN_MS
        ) {
            lastNotificationTime = now
            nextNotifyMinute += notifyAfterMinutes

            NotificationHelper.showMindfulNotification(
                applicationContext,
                currentMinutes
            )
        }

        scheduleSessionTimeout()
    }

    // ─────────────────────────────────────────────────────────────

    private fun scheduleSessionTimeout() {
        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = Runnable { endSession() }
        handler.postDelayed(endSessionRunnable!!, SESSION_RESET_MS)
    }

    private fun endSession() {
        if (sessionStartTime == 0L) return

        val remaining = accumulatedSessionTime - lastPersistedMs
        if (remaining >= 60_000L) {
            CoroutineScope(Dispatchers.IO).launch {
                UsageDataStore.addSessionTime(applicationContext, remaining)
            }
        }

        unconsciousDetector.resetSession()

        sessionStartTime = 0L
        lastEventTime = 0L
        accumulatedSessionTime = 0L
        lastPersistedMs = 0L
        rapidScrollStreak = 0
        lastRapidScrollTime = 0L
        lastUnconsciousNotificationTime = 0L

        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = null
    }

    override fun onInterrupt() = endSession()

    override fun onDestroy() {
        endSession()
        super.onDestroy()
    }
}
