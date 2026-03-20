package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.work.*
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.UsageDataStore
import com.example.myapplication.data.analytics.UsageRepository
import com.example.myapplication.data.sync.SyncWorker
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
    private var isSessionActive = false
    private var accumulatedSessionTime = 0L
    private var lastPersistedMs = 0L

    private val SESSION_RESET_MS = 60_000L

    private val handler = Handler(Looper.getMainLooper())
    private var endSessionRunnable: Runnable? = null

    private lateinit var unconsciousDetector: UnconsciousScrollingDetector

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Guards against duplicate endSession calls
    private var lastSessionEndTime = 0L
    private val MIN_TIME_BETWEEN_ENDS_MS = 2500L  // increased to 2.5 seconds
    private var sessionIncrementedForCurrentSession = false  // Renamed to be clearer

    // Add a Job to collect setting changes
    private var settingsCollectorJob: Job? = null

    /* ================================================= */

    override fun onServiceConnected() {
        super.onServiceConnected()
        usageRepository = UsageRepository(this)
        unconsciousDetector = UnconsciousScrollingDetector(applicationContext)

        // Load initial value and start collecting changes
        serviceScope.launch {
            val initialLimit = NotificationSettingsStore.getNotifyAfterReels(applicationContext)
            notifyAfterReels = initialLimit.coerceAtLeast(1)
            nextNotifyReel = notifyAfterReels

            // Start collecting setting changes
            startCollectingSettings()
        }
    }

    /* ================================================= */

    private fun startCollectingSettings() {
        settingsCollectorJob?.cancel()

        settingsCollectorJob = serviceScope.launch {
            NotificationSettingsStore.notifyAfterReelsFlow(applicationContext)
                .collect { newValue ->
                    val previousValue = notifyAfterReels
                    val newThreshold = newValue.coerceAtLeast(1)

                    if (previousValue != newThreshold) {
                        notifyAfterReels = newThreshold

                        // Recalculate next notification threshold intelligently
                        // This ensures we don't lose progress or spam notifications
                        if (reelsViewed < nextNotifyReel) {
                            // Still within current threshold, adjust target
                            val progress = reelsViewed % previousValue
                            nextNotifyReel = reelsViewed + (notifyAfterReels - progress)
                        } else {
                            // Already past threshold, set next based on current value
                            nextNotifyReel = reelsViewed + notifyAfterReels
                        }

                        Log.d("Settings", "Notification threshold updated from $previousValue to $notifyAfterReels reels")
                        Log.d("Settings", "Current reels: $reelsViewed, Next notify at: $nextNotifyReel")
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

        return pkg == "com.instagram.android" &&
                (className.contains("ViewPager") ||
                        className.contains("RecyclerView"))
    }

    /* ================================================= */

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        // EXIT DETECTION - ignore system UI noise
        if (
            (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) &&
            pkg != "com.instagram.android" &&
            pkg != "android" &&
            pkg != "com.android.systemui" &&
            pkg != "com.android.launcher3"  // common launcher packages
        ) {
            if (isSessionActive) {
                Log.d("Session", "App exit detected (pkg=$pkg, type=${event.eventType}) → ending session")
                endSession()
            }
            return
        }

        if (pkg != "com.instagram.android") return

        val prevTime = lastEventTime

        if (lastEventTime != 0L && now - lastEventTime > SESSION_RESET_MS) {
            Log.d("Session", "Inactivity timeout → ending session")
            endSession()
        }

        handleSessionTime(now)

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
        if (!isSessionActive) {
            isSessionActive = true
            sessionStartTime = now
            lastEventTime = now
            accumulatedSessionTime = 0L
            lastPersistedMs = 0L
            // Reset the session increment flag when starting a new session
            sessionIncrementedForCurrentSession = false
            return
        }

        val delta = now - lastEventTime
        accumulatedSessionTime += delta
        lastEventTime = now

        val deltaSincePersist = accumulatedSessionTime - lastPersistedMs

        // Save every 30 seconds instead of 60 seconds for more granular updates
        if (deltaSincePersist >= 30_000L) {
            serviceScope.launch {
                UsageDataStore.addSessionTime(applicationContext, deltaSincePersist)
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
                notifyAfterReels  // Changed from nextNotifyReel to notifyAfterReels
            )
            nextNotifyReel += notifyAfterReels

            Log.d("Notifications", "Notification sent for $notifyAfterReels reels")
            Log.d("Notifications", "Next notification at: $nextNotifyReel")
        }
    }

    /* ================================================= */

    private fun scheduleSessionTimeout() {
        endSessionRunnable?.let { handler.removeCallbacks(it) }

        endSessionRunnable = Runnable {
            if (isSessionActive) {
                Log.d("Session", "Timeout triggered → ending session")
                endSession()
            }
        }

        handler.postDelayed(endSessionRunnable!!, SESSION_RESET_MS)
    }

    /* ================================================= */

    override fun onDestroy() {
        Log.d("Service", "onDestroy called → forcing session end")
        settingsCollectorJob?.cancel()  // Clean up the collector
        endSession()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun triggerImmediateSync() {
        try {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    "session_end_sync",
                    ExistingWorkPolicy.REPLACE,
                    request
                )

            Log.d("Sync", "Immediate sync enqueued")
        } catch (e: Exception) {
            Log.e("Sync", "Failed to enqueue sync", e)
        }
    }

    private fun endSession() {
        val now = System.currentTimeMillis()

        if (!isSessionActive) return

        if (now - lastSessionEndTime < MIN_TIME_BETWEEN_ENDS_MS) {
            Log.d("Session", "Duplicate endSession call ignored (${now - lastSessionEndTime}ms)")
            return
        }

        lastSessionEndTime = now

        // Only increment session count if we haven't already for this session
        val shouldIncrementSession = !sessionIncrementedForCurrentSession

        isSessionActive = false

        val remainingMs = accumulatedSessionTime - lastPersistedMs

        // Always save remaining time, even if it's less than 10 seconds
        // But ensure it's at least 1 second to avoid noise
        val finalRemainingMs = if (remainingMs > 0) remainingMs.coerceAtLeast(1000L) else 0L

        // Write to BOTH storages
        serviceScope.launch {
            try {
                // 1. Room (for sync + structured queries)
                if (finalRemainingMs >= 1000L) {  // Save any meaningful duration (>= 1 second)
                    usageRepository.addSessionTime(finalRemainingMs)
                }

                // Only increment session count once per session
                if (shouldIncrementSession) {
                    usageRepository.incrementSession()
                    sessionIncrementedForCurrentSession = true
                    Log.d("DB_WRITE", "Session count incremented for this session")
                }

                // 2. DataStore (for quick local UI display)
                if (finalRemainingMs >= 1000L) {  // Save any meaningful duration
                    UsageDataStore.addSessionTime(applicationContext, finalRemainingMs)
                    Log.d("DB_WRITE", "Saved $finalRemainingMs ms to DataStore")
                }

                Log.d("DB_WRITE", "Final session persisted to Room + DataStore | final ms: $finalRemainingMs | incremented: $shouldIncrementSession")
            } catch (e: Exception) {
                Log.e("DB_WRITE", "Final persistence failed", e)
            }
        }

        triggerImmediateSync()

        Log.d("Session", "Session ended | sync requested | total min: ${accumulatedSessionTime / 60000} | remaining ms: $remainingMs")

        // Reset everything
        unconsciousDetector.resetSession()
        deepScrollRecordedThisSession = false
        sessionStartTime = 0L
        lastEventTime = 0L
        accumulatedSessionTime = 0L
        lastPersistedMs = 0L
        reelsViewed = 0
        nextNotifyReel = notifyAfterReels  // Uses the latest value
        lastReelEventTime = 0L
        lastIndex = -1
        isFirstIndexIgnored = false
        lastScrollY = 0

        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = null

        // Note: We don't reset sessionIncrementedForCurrentSession here
        // because it will be reset when a new session starts in handleSessionTime()
    }

    override fun onInterrupt() {
        Log.w("Service", "onInterrupt → ending session")
        endSession()
    }
}