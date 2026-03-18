package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.UsageDataStore
import com.example.myapplication.data.analytics.UsageRepository
import com.example.myapplication.service.detector.UnconsciousScrollingDetector
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

    private var nextNotifyReel = 5

    /* ================= INDEX TRACKING ================= */

    private var lastIndex = -1
    private var isFirstIndexIgnored = false

    /* ================= DELTA FALLBACK ================= */

    private var lastScrollY = 0

    /* ================= SESSION ================= */

    private var deepScrollRecordedThisSession = false

    private var sessionStartTime = 0L
    private var lastEventTime = 0L
    private var accumulatedSessionTime = 0L
    private var lastPersistedMs = 0L

    private val SESSION_RESET_MS = 5 * 60_000L

    private val handler = Handler(Looper.getMainLooper())
    private var endSessionRunnable: Runnable? = null

    private lateinit var unconsciousDetector: UnconsciousScrollingDetector

    /* ================= COROUTINE ================= */

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /* ================================================= */

    override fun onServiceConnected() {
        super.onServiceConnected()

        usageRepository = UsageRepository(this)
        unconsciousDetector = UnconsciousScrollingDetector(applicationContext)

        serviceScope.launch {
            val initialLimit =
                NotificationSettingsStore.getNotifyAfterReels(applicationContext)

            notifyAfterReels = initialLimit.coerceAtLeast(1)
            nextNotifyReel = notifyAfterReels
        }
    }

    /* ================================================= */

    private fun isReelsScroll(event: AccessibilityEvent): Boolean {

        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED)
            return false

        val pkg = event.packageName?.toString() ?: return false
        val className = event.className?.toString() ?: return false

        return pkg == "com.instagram.android" &&
                (className.contains("ViewPager") ||
                        className.contains("RecyclerView"))
    }

    /* ================================================= */

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        if (pkg != "com.instagram.android") return

        // 🔥 store previous time FIRST
        val prevTime = lastEventTime

        // 🔥 reset session if idle
        if (lastEventTime != 0L && now - lastEventTime > SESSION_RESET_MS) {
            endSession()
        }

        // 🔥 update session time
        handleSessionTime(now)

        // 🔥 ALWAYS run detector
        unconsciousDetector.onAccessibilityEvent(event, now, prevTime)

        /* ================= REEL DETECTION ================= */

        if (isReelsScroll(event)) {

            val toIndex = event.toIndex
            val currentY = event.scrollY

            var isForwardScroll = false

            if (toIndex != -1) {

                if (!isFirstIndexIgnored) {
                    lastIndex = toIndex
                    isFirstIndexIgnored = true
                    return
                }

                isForwardScroll = toIndex > lastIndex
                lastIndex = toIndex

            } else {

                val deltaY = currentY - lastScrollY
                lastScrollY = currentY
                isForwardScroll = deltaY > 100
            }

            if (!isForwardScroll) return

            if (now - lastReelEventTime > REEL_COOLDOWN_MS) {

                reelsViewed++
                lastReelEventTime = now

                serviceScope.launch {
                    usageRepository.incrementReelsViewed()
                }

                checkReelNotification()
            }
        }

        /* ================= DEEP SCROLL ================= */

        if (
            unconsciousDetector.hasDetectedUnconsciousScrolling() &&
            !deepScrollRecordedThisSession
        ) {
            deepScrollRecordedThisSession = true

            serviceScope.launch {

                Log.d("DeepScroll", "Triggered")

                UsageDataStore.incrementDeepScroll(applicationContext)
                usageRepository.incrementDeepScroll()
            }
        }

        scheduleSessionTimeout()
    }

    /* ================================================= */

    private fun handleSessionTime(now: Long) {

        if (sessionStartTime == 0L) {
            sessionStartTime = now
            lastEventTime = now
            accumulatedSessionTime = 0L
            lastPersistedMs = 0L
            return
        }

        val delta = now - lastEventTime
        accumulatedSessionTime += delta
        lastEventTime = now

        val deltaSincePersist = accumulatedSessionTime - lastPersistedMs

        if (deltaSincePersist >= 60_000L) {

            serviceScope.launch {

                // UI
                UsageDataStore.addSessionTime(applicationContext, deltaSincePersist)

                // DB
                usageRepository.addSessionTime(deltaSincePersist)
            }

            lastPersistedMs += deltaSincePersist
        }
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

        // 🔥 Save remaining time
        val remaining = accumulatedSessionTime - lastPersistedMs

        if (remaining >= 60_000L) {
            serviceScope.launch {

                UsageDataStore.addSessionTime(applicationContext, remaining)

                usageRepository.addSessionTime(remaining)
            }
        }

        unconsciousDetector.resetSession()
        deepScrollRecordedThisSession = false

        sessionStartTime = 0L
        lastEventTime = 0L
        accumulatedSessionTime = 0L
        lastPersistedMs = 0L

        reelsViewed = 0
        nextNotifyReel = notifyAfterReels
        lastReelEventTime = 0L

        lastIndex = -1
        isFirstIndexIgnored = false
        lastScrollY = 0

        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = null
    }

    override fun onInterrupt() = endSession()

    override fun onDestroy() {
        serviceScope.cancel()
        endSession()
        super.onDestroy()
    }
}