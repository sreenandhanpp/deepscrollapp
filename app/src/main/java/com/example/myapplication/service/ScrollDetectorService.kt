package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.UsageDataStore
import com.example.myapplication.data.analytics.UsageRepository
import com.example.myapplication.data.sync.SyncWorker
import com.example.myapplication.service.detector.UnconsciousScrollingDetector
import com.example.myapplication.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
    private var lastSettledPage = -1  // for scrollY fallback

    private val screenHeight by lazy { resources.displayMetrics.heightPixels }
    private var isFirstIndexIgnored = false

    /* ================= DELTA FALLBACK ================= */
    private var lastScrollY = 0

    /* ================= SESSION ================= */

    /* ================= REEL TIME TRACKING ================= */
    private var reelTimeStart = 0L
    private var accumulatedReelTimeMs = 0L
    private var lastReelScrollTime = 0L
    private val REEL_IDLE_TIMEOUT_MS = 3000L  // stop counting if no scroll for 3s
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
    private val MIN_TIME_BETWEEN_ENDS_MS = 2500L
    private var sessionIncrementedForCurrentSession = false

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
                        if (reelsViewed < nextNotifyReel) {
                            val progress = reelsViewed % previousValue
                            nextNotifyReel = reelsViewed + (notifyAfterReels - progress)
                        } else {
                            nextNotifyReel = reelsViewed + notifyAfterReels
                        }

                        Log.d(
                            "Settings",
                            "Notification threshold updated from $previousValue to $notifyAfterReels reels"
                        )
                        Log.d(
                            "Settings",
                            "Current reels: $reelsViewed, Next notify at: $nextNotifyReel"
                        )
                    }
                }
        }
    }

    /* ================================================= */

    private fun isReelsScroll(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false

        val pkg = event.packageName?.toString() ?: return false
        val className = event.className?.toString() ?: return false

        Log.d(
            "ClassSniffer",
            "className=$className | toIndex=${event.toIndex} | scrollY=${event.scrollY}"
        )

        // Strict match only ViewPager - no other Instagram RecyclerViews
        return pkg == "com.instagram.android" &&
                className == "androidx.viewpager.widget.ViewPager"
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
            pkg != "com.android.launcher3"
        ) {
            if (isSessionActive) {
                Log.d(
                    "Session",
                    "App exit detected (pkg=$pkg, type=${event.eventType}) → ending session"
                )
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
        /* ================= REEL DETECTION ================= */
        if (isReelsScroll(event)) {
            val toIndex = event.toIndex
            val scrollY = event.scrollY

            if (toIndex != -1) {
                // toIndex path — most accurate, fires once per settled page
                if (lastIndex == -1) {
                    lastIndex = toIndex  // initialize, don't count
                    return
                }

                val diff = toIndex - lastIndex
                if (diff > 0) {
                    lastIndex = toIndex
                    reelsViewed += diff  // handles skip-jumps

                    Log.d("Reel Counter", "toIndex path | +$diff | total: $reelsViewed")

                    repeat(diff) {
                        serviceScope.launch { usageRepository.incrementReelsViewed() }
                    }
                    checkReelNotification()
                }

            } else if (screenHeight > 0) {
                // scrollY fallback — compute settled page from pixel offset
                val currentPage = scrollY / screenHeight

                if (lastSettledPage == -1) {
                    lastSettledPage = currentPage  // initialize
                    return
                }

                val diff = currentPage - lastSettledPage
                if (diff > 0) {
                    lastIndex = toIndex
                    reelsViewed += diff

                    handleReelTime(now)   // ← add this line only

                    Log.d("Reel Counter", "toIndex path | +$diff | total: $reelsViewed")

                    repeat(diff) {
                        serviceScope.launch { usageRepository.incrementReelsViewed() }
                    }
                    checkReelNotification()
                }
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
            sessionIncrementedForCurrentSession = false
            return
        }

        val delta = now - lastEventTime
        accumulatedSessionTime += delta
        lastEventTime = now

        val deltaSincePersist = accumulatedSessionTime - lastPersistedMs

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

            // Send cumulative milestone
            NotificationHelper.showReelReminderNotification(
                applicationContext,
                nextNotifyReel
            )

            // Move next target forward
            nextNotifyReel += notifyAfterReels

            Log.d(
                "ReelNotification",
                "Next notification at: $nextNotifyReel"
            )
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
        settingsCollectorJob?.cancel()
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

    private fun handleReelTime(now: Long) {
        if (lastReelScrollTime == 0L) {
            reelTimeStart = now
            lastReelScrollTime = now
            return
        }

        val gapSinceLastScroll = now - lastReelScrollTime

        if (gapSinceLastScroll > REEL_IDLE_TIMEOUT_MS) {
            val chunk = lastReelScrollTime - reelTimeStart
            if (chunk > 0) {
                accumulatedReelTimeMs += chunk
                val toFlush = accumulatedReelTimeMs
                accumulatedReelTimeMs = 0L
                serviceScope.launch {
                    usageRepository.addSessionTime(toFlush)  // ← existing method
                    Log.d("ReelTime", "Flushed reel chunk: ${toFlush / 1000}s")
                }
            }
            reelTimeStart = now
        }

        lastReelScrollTime = now
    }

    private fun endSession() {
        val now = System.currentTimeMillis()

        if (!isSessionActive) return

        if (now - lastSessionEndTime < MIN_TIME_BETWEEN_ENDS_MS) {
            Log.d("Session", "Duplicate endSession call ignored (${now - lastSessionEndTime}ms)")
            return
        }

        lastSessionEndTime = now

        val shouldIncrementSession = !sessionIncrementedForCurrentSession

        isSessionActive = false

        val remainingMs = accumulatedSessionTime - lastPersistedMs
        val finalRemainingMs = if (remainingMs > 0) remainingMs.coerceAtLeast(1000L) else 0L
        if (reelTimeStart > 0L && lastReelScrollTime > reelTimeStart) {
            val finalChunk = (lastReelScrollTime - reelTimeStart) + accumulatedReelTimeMs
            if (finalChunk >= 1000L) {
                serviceScope.launch {
                    usageRepository.addSessionTime(finalChunk)  // ← existing method
                    Log.d("ReelTime", "Final reel time: ${finalChunk / 1000}s")
                }
            }
        }
        serviceScope.launch {
            try {
                if (finalRemainingMs >= 1000L) {
                    usageRepository.addSessionTime(finalRemainingMs)
                }

                if (shouldIncrementSession) {
                    usageRepository.incrementSession()
                    sessionIncrementedForCurrentSession = true
                    Log.d("DB_WRITE", "Session count incremented for this session")
                }

                if (finalRemainingMs >= 1000L) {
                    UsageDataStore.addSessionTime(applicationContext, finalRemainingMs)
                    Log.d("DB_WRITE", "Saved $finalRemainingMs ms to DataStore")
                }

                Log.d(
                    "DB_WRITE",
                    "Final session persisted | final ms: $finalRemainingMs | incremented: $shouldIncrementSession"
                )
            } catch (e: Exception) {
                Log.e("DB_WRITE", "Final persistence failed", e)
            }
        }

        triggerImmediateSync()

        Log.d(
            "Session",
            "Session ended | total reels in session: $reelsViewed | total time: ${accumulatedSessionTime / 60000} min"
        )

        // Reset everything
        unconsciousDetector.resetSession()
        deepScrollRecordedThisSession = false
        sessionStartTime = 0L
        lastEventTime = 0L
        accumulatedSessionTime = 0L
        lastPersistedMs = 0L
        reelsViewed = 0  // Reset for next session
        nextNotifyReel = notifyAfterReels
        lastReelEventTime = 0L
        lastIndex = -1
        lastSettledPage = -1   // ← add this
        isFirstIndexIgnored = false
        lastScrollY = 0
        lastReelEventTime = 0L
        isFirstIndexIgnored = false
        lastScrollY = 0
        reelTimeStart = 0L
        lastReelScrollTime = 0L
        accumulatedReelTimeMs = 0L

        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = null
    }

    override fun onInterrupt() {
        Log.w("Service", "onInterrupt → ending session")
        endSession()
    }
}