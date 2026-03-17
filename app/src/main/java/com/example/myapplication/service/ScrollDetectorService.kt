package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.UsageDataStore
import com.example.myapplication.data.analytics.UsageRepository
import com.example.myapplication.service.detector.UnconsciousScrollingDetector
import com.example.myapplication.service.detector.UnconsciousType
import com.example.myapplication.utils.NotificationHelper
import kotlinx.coroutines.*
import kotlin.math.abs

class ScrollDetectorService : AccessibilityService() {

    /* ================= REELS ================= */

    private var reelsViewed = 0
    private var notifyAfterReels = 5

    private lateinit var usageRepository: UsageRepository

    private var lastReelEventTime = 0L
    private val REEL_COOLDOWN_MS = 700L

    /* milestone tracker */
    private var nextNotifyReel = 5

    /* ================= SESSION ================= */

    private var deepScrollRecordedThisSession = false

    private var sessionStartTime = 0L
    private var lastEventTime = 0L
    private var accumulatedSessionTime = 0L
    private var lastPersistedMs = 0L

    private val SESSION_RESET_MS = 5 * 60_000L
    private val MIN_SESSION_MS = 2_000L

    /* ================= RAPID SCROLL ================= */

    private var rapidScrollStreak = 0
    private var lastRapidScrollTime = 0L

    private val RAPID_SCROLL_THRESHOLD_MS = 500L
    private val RAPID_SCROLL_STREAK_LIMIT = 3
    private val UNCONSCIOUS_COOLDOWN_MS = 45_000L
    private var lastUnconsciousNotificationTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var endSessionRunnable: Runnable? = null

    private lateinit var unconsciousDetector: UnconsciousScrollingDetector

    /* ================================================= */

    override fun onServiceConnected() {
        super.onServiceConnected()
        usageRepository = UsageRepository(this)
        unconsciousDetector = UnconsciousScrollingDetector(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {

            // 1️⃣ Load saved value immediately
            val initialLimit =
                NotificationSettingsStore.getNotifyAfterReels(applicationContext)

            notifyAfterReels = initialLimit.coerceAtLeast(1)
            nextNotifyReel = notifyAfterReels

            Log.d("ReelsSetting", "Initial limit → $notifyAfterReels")

            // 2️⃣ Listen for future changes
            NotificationSettingsStore
                .notifyAfterReelsFlow(applicationContext)
                .collect { newLimit ->

                    val limit = newLimit.coerceAtLeast(1)

                    if (limit != notifyAfterReels) {

                        notifyAfterReels = limit

                        // Reset session counters
                        reelsViewed = 0
                        nextNotifyReel = notifyAfterReels
                        lastReelEventTime = 0L

                        Log.d("ReelsSetting", "Limit changed → $notifyAfterReels")
                    }
                }
        }
    }

    /* ================================================= */

    private fun isReelsScroll(event: AccessibilityEvent): Boolean {

        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED)
            return false

        val pkg = event.packageName?.toString() ?: return false
        val className = event.className?.toString() ?: return false

        if (pkg != "com.instagram.android")
            return false

        if (className != "androidx.viewpager.widget.ViewPager")
            return false

        return true
    }

    /* ================================================= */

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        // Ignore non-Instagram events
        if (pkg != "com.instagram.android") {
            return
        }

        // Update last event time FIRST
        if (lastEventTime == 0L) {
            lastEventTime = now
        }

        // Reset session if idle for too long
        if (now - lastEventTime > SESSION_RESET_MS) {
            endSession()
        }

        // Update event time again for current event
        lastEventTime = now

        val isScrollEvent =
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        /* ================= REEL DETECTION ================= */

        if (isReelsScroll(event)) {

            if (now - lastReelEventTime > REEL_COOLDOWN_MS) {

                reelsViewed++
                lastReelEventTime = now

                CoroutineScope(Dispatchers.IO).launch {
                    usageRepository.incrementReelsViewed()
                }

                Log.d("ReelsCounter", "Reels viewed: $reelsViewed")

                checkReelNotification()
            }

            unconsciousDetector.updateLastEventTime(now)
            unconsciousDetector.onAccessibilityEvent(event, now)
        }

        /* ================= DEEP SCROLL ================= */

        if (
            unconsciousDetector.hasDetectedUnconsciousScrolling() &&
            !deepScrollRecordedThisSession
        ) {
            deepScrollRecordedThisSession = true
            CoroutineScope(Dispatchers.IO).launch {
                usageRepository.incrementDeepScroll()
            }
        }

        /* ================= FILTER ================= */

        if (isScrollEvent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) return
            if (event.scrollDeltaY == 0 && event.maxScrollY == 0) return
        }

        /* ================= SESSION ================= */

        if (lastEventTime != 0L && now - lastEventTime > SESSION_RESET_MS) {
            endSession()
        }

        if (sessionStartTime == 0L) {

            sessionStartTime = now
            lastEventTime = now
            accumulatedSessionTime = 0L
            lastPersistedMs = 0L
            rapidScrollStreak = 0
            lastRapidScrollTime = 0L

            return
        }

        val delta = now - lastEventTime
        accumulatedSessionTime += delta
        lastEventTime = now

        val deltaSincePersist = accumulatedSessionTime - lastPersistedMs

        if (deltaSincePersist >= 60_000L) {
            CoroutineScope(Dispatchers.IO).launch {
                UsageDataStore.addSessionTime(applicationContext, deltaSincePersist)
            }
            lastPersistedMs += deltaSincePersist
        }

        /* ================= RAPID SCROLL ================= */

        if (isScrollEvent) {

            val sinceLast = now - lastRapidScrollTime

            rapidScrollStreak =
                if (sinceLast < RAPID_SCROLL_THRESHOLD_MS)
                    rapidScrollStreak + 1
                else
                    1

            lastRapidScrollTime = now

            if (
                accumulatedSessionTime >= 2 * 60_000L &&
                rapidScrollStreak >= RAPID_SCROLL_STREAK_LIMIT &&
                now - lastUnconsciousNotificationTime > UNCONSCIOUS_COOLDOWN_MS
            ) {

                lastUnconsciousNotificationTime = now

                NotificationHelper.showUnconsciousScrollingNotification(
                    applicationContext,
                    UnconsciousType.RAPID_SWIPING
                )
            }
        }

        if (accumulatedSessionTime < MIN_SESSION_MS) {
            scheduleSessionTimeout()
            return
        }

        scheduleSessionTimeout()
    }

    /* ================================================= */

    private fun checkReelNotification() {

        if (reelsViewed >= nextNotifyReel) {

            NotificationHelper.showReelReminderNotification(
                applicationContext,
                nextNotifyReel
            )

            nextNotifyReel += notifyAfterReels
        }
    }

    /* ================================================= */

    private fun scheduleSessionTimeout() {

        endSessionRunnable?.let { handler.removeCallbacks(it) }

        endSessionRunnable = Runnable { endSession() }

        handler.postDelayed(endSessionRunnable!!, SESSION_RESET_MS)
    }

    /* ================================================= */

    private fun endSession() {

        if (sessionStartTime == 0L) return

        val remaining = accumulatedSessionTime - lastPersistedMs

        if (remaining >= 60_000L) {
            CoroutineScope(Dispatchers.IO).launch {
                UsageDataStore.addSessionTime(applicationContext, remaining)
            }
        }

        unconsciousDetector.resetSession()

        deepScrollRecordedThisSession = false

        sessionStartTime = 0L
        lastEventTime = 0L
        accumulatedSessionTime = 0L
        lastPersistedMs = 0L
        rapidScrollStreak = 0
        lastRapidScrollTime = 0L
        lastUnconsciousNotificationTime = 0L

        reelsViewed = 0
        nextNotifyReel = notifyAfterReels
        lastReelEventTime = 0L

        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = null
    }

    override fun onInterrupt() = endSession()

    override fun onDestroy() {
        endSession()
        super.onDestroy()
    }
}